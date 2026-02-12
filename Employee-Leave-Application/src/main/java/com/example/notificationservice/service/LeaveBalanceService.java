package com.example.notificationservice.service;

import com.example.notificationservice.dto.LeaveBalanceResponse;
import com.example.notificationservice.dto.LeaveTypeBreakdown;
import com.example.notificationservice.entity.*;
import com.example.notificationservice.enums.LeaveStatus;
import com.example.notificationservice.enums.LeaveType;
import com.example.notificationservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LeaveBalanceService {

    // ===== REPOSITORIES =====
    private final CarryForwardBalanceRepository carryForwardBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LossOfPayRecordRepository lossOfPayRecordRepository;
    private final CompOffService compOffService;

    // ===== BUSINESS CONSTANTS =====
    private static final double MONTHLY_ALLOCATED = 2.0;
    private static final double MAX_CARRY_FORWARD = 5.0;
    private static final double LOSS_OF_PAY_INCREMENT = 1.0;

    // =====================================================
    // READ: DASHBOARD / BALANCE (EXISTING CODE - KEPT AS IS)
    // =====================================================
    public LeaveBalanceResponse getBalance(Long employeeId, Integer year) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        List<LeaveAllocation> allocations =
                allocationRepository.findByEmployeeIdAndYear(employeeId, year);

        List<LeaveApplication> approvedLeaves =
                leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                        employeeId, LeaveStatus.APPROVED, year
                );

        Map<LeaveType, List<LeaveApplication>> byType =
                approvedLeaves.stream()
                        .collect(Collectors.groupingBy(LeaveApplication::getLeaveType));

        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();
        double totalAllocated = 0;
        double totalUsed = 0;

        for (LeaveAllocation alloc : allocations) {

            LeaveType type = LeaveType.valueOf(alloc.getLeaveCategory());

            // NOTE: getCarriedForwardDays() now always returns 0.0
            // Carry forward is stored separately in carry_forward_balance table
            double allocated = alloc.getAllocatedDays() + alloc.getCarriedForwardDays();

            double used = byType.getOrDefault(type, List.of())
                    .stream()
                    .map(LeaveApplication::getDays)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();

            long halfDays = byType.getOrDefault(type, List.of())
                    .stream()
                    .filter(l -> l.getDays().compareTo(new BigDecimal("0.5")) == 0)
                    .count();

            breakdown.add(new LeaveTypeBreakdown(
                    type,
                    allocated,
                    used,
                    allocated - used,
                    (int) halfDays
            ));

            totalAllocated += allocated;
            totalUsed += used;
        }

        BigDecimal compOffBalance = compOffService.getAvailableCompOffDays(employeeId);

        breakdown.add(new LeaveTypeBreakdown(
                LeaveType.COMP_OFF,
                compOffBalance.doubleValue(),
                0.0,
                compOffBalance.doubleValue(),
                0
        ));

        double remaining = totalAllocated - totalUsed;

        int currentMonth = LocalDate.now().getMonthValue();
        long currentMonthApproved = approvedLeaves.stream()
                .filter(l -> l.getStartDate().getMonthValue() == currentMonth)
                .count();

        Double lopPercentage =
                lossOfPayRecordRepository.getTotalLossPercentageByEmployeeIdAndYear(employeeId, year);

        LeaveBalanceResponse response = new LeaveBalanceResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setYear(year);
        response.setTotalAllocated(totalAllocated);
        response.setTotalUsed(totalUsed);
        response.setTotalRemaining(remaining);
        response.setCompOffBalance(compOffBalance.doubleValue());
        response.setEligibleToCarry(Math.min(Math.max(remaining, 0), MAX_CARRY_FORWARD));
        response.setCurrentMonthApproved((int) currentMonthApproved);
        response.setExceededMonthlyLimit(currentMonthApproved > MONTHLY_ALLOCATED);
        response.setLopPercentage(lopPercentage != null ? lopPercentage : 0.0);
        response.setBreakdown(breakdown);
        response.setTotalWorkingDays(employee.getTotalWorkingDays());

        return response;
    }

    // =====================================================
    // WRITE: MONTHLY APPROVED LEAVE PROCESSING (NEW LOGIC)
    // =====================================================
    /**
     * MONTHLY APPROVED LEAVE PROCESS
     *
     * Flow:
     * 1. Calculate excess = approvedDays - monthlyAllocated (2)
     * 2. If excess > 0:
     *    a. Check carry forward availability (COMMON pool)
     *    b. Use carry forward first (update totalUsed and remaining)
     *    c. If carry forward insufficient → apply loss of pay (1% per day)
     * 3. Loss of pay accumulates across months
     */
    @Transactional
    public void processMonthlyApprovedLeave(
            Long employeeId, Double approvedDays, Integer year, Integer month) {

        log.info("\n=== MONTHLY LEAVE PROCESSING ===");
        log.info("Employee ID: {}", employeeId);
        log.info("Approved Days: {}", approvedDays);
        log.info("Year: {}, Month: {}", year, month);
        log.info("Monthly Allocated: {}", MONTHLY_ALLOCATED);

        // STEP 1: Calculate excess
        double excessBalance = approvedDays - MONTHLY_ALLOCATED;

        log.info("Excess Balance: {} - {} = {}", approvedDays, MONTHLY_ALLOCATED, excessBalance);

        if (excessBalance <= 0) {
            log.info("✓ No excess (approved days within monthly allocation)");
            return;
        }

        log.info("→ Excess detected! Processing excess of {} days", excessBalance);

        // ===================================================================
        // STEP 2: CHECK CARRY FORWARD AVAILABILITY (COMMON POOL)
        // ===================================================================
        Optional<CarryForwardBalance> carryForwardOpt =
                carryForwardBalanceRepository.findByEmployeeIdAndYear(employeeId, year);

        if (carryForwardOpt.isPresent() && carryForwardOpt.get().getRemaining() > 0) {

            // CARRY FORWARD IS AVAILABLE (COMMON - NOT CATEGORY SPECIFIC)
            CarryForwardBalance carryForward = carryForwardOpt.get();
            double availableCarryForward = carryForward.getRemaining();

            log.info("✓ Carry Forward Available (COMMON POOL)!");
            log.info("  Total Carried Forward: {}", carryForward.getTotalCarriedForward());
            log.info("  Already Used: {}", carryForward.getTotalUsed());
            log.info("  Remaining: {}", availableCarryForward);

            // ===================================================================
            // STEP 3: USE CARRY FORWARD FIRST (BEFORE LOSS OF PAY)
            // ===================================================================
            double usableFromCarryForward = Math.min(excessBalance, availableCarryForward);

            carryForward.setTotalUsed(carryForward.getTotalUsed() + usableFromCarryForward);
            carryForward.setRemaining(carryForward.getRemaining() - usableFromCarryForward);
            carryForwardBalanceRepository.save(carryForward);

            log.info("✓ Used {} days from COMMON carry forward", usableFromCarryForward);
            log.info("  Updated Total Used: {}", carryForward.getTotalUsed());
            log.info("  Updated Remaining: {}", carryForward.getRemaining());

            // Update excess after using carry forward
            excessBalance -= usableFromCarryForward;

            if (excessBalance > 0) {
                log.info("→ Remaining excess after carry forward: {} days", excessBalance);
                log.info("→ Applying loss of pay for remaining excess");
            } else {
                log.info("  Loss of Pay: 0% (carry forward covered all excess)");
            }
        } else {
            // NO CARRY FORWARD AVAILABLE
            log.info("✗ No carry forward available (remaining = 0)");
            log.info("→ Applying loss of pay for entire excess of {} days", excessBalance);
        }

        // ===================================================================
        // STEP 4: APPLY LOSS OF PAY (IF STILL EXCESS AFTER CARRY FORWARD)
        // ===================================================================
        if (excessBalance > 0) {
            applyLossOfPay(employeeId, year, month, excessBalance);
        }

        log.info("=== MONTHLY LEAVE PROCESSING COMPLETE ===\n");
    }

    // =====================================================
    // WRITE: YEAR END CARRY FORWARD (NEW LOGIC)
    // =====================================================
    /**
     * Process year-end carry forward
     * Rule: If yearly balance <= 5, carry forward to next year (max 5)
     * Carry forward is stored as COMMON pool (not category-specific)
     */
    @Transactional
    public void processYearEndCarryForward(
            Long employeeId, Integer currentYear, Double yearlyBalance) {

        log.info("[CARRY-FORWARD] Processing for employee {}: year={}, balance={}",
                employeeId, currentYear, yearlyBalance);

        double carryAmount = Math.min(Math.max(yearlyBalance, 0), MAX_CARRY_FORWARD);

        if (carryAmount == 0) {
            log.info("[CARRY-FORWARD] No balance to carry forward");
            return;
        }

        CarryForwardBalance carry =
                carryForwardBalanceRepository
                        .findByEmployeeIdAndYear(employeeId, currentYear + 1)
                        .orElse(new CarryForwardBalance());

        carry.setEmployeeId(employeeId);
        carry.setYear(currentYear + 1);
        carry.setTotalCarriedForward(carryAmount);
        carry.setTotalUsed(0.0);
        carry.setRemaining(carryAmount);

        carryForwardBalanceRepository.save(carry);

        log.info("[CARRY-FORWARD] Carried forward {} days to year {}",
                carryAmount, currentYear + 1);
    }

    // =====================================================
    // READ: GET CARRY FORWARD BALANCE
    // =====================================================
    public CarryForwardBalance getCarryForwardBalance(Long employeeId, Integer year) {
        return carryForwardBalanceRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElse(null);
    }

    // =====================================================
    // READ: TOTAL LOSS OF PAY (FOR CONTROLLER/DASHBOARD)
    // =====================================================
    public Double getTotalLossOfPayPercentage(Long employeeId, Integer year) {
        Double total =
                lossOfPayRecordRepository
                        .getTotalLossPercentageByEmployeeIdAndYear(employeeId, year);

        return total != null ? total : 0.0;
    }

    // =====================================================
    // PRIVATE: LOSS OF PAY APPLICATION
    // =====================================================
    /**
     * Apply loss of pay - called ONLY after carry forward is checked
     * Rule: 1% per excess day
     * Accumulates across months (e.g., Month1: 2% + Month2: 3% = Total: 5%)
     */
    private void applyLossOfPay(
            Long employeeId, Integer year, Integer month, Double excessDays) {

        log.info("  === LOSS OF PAY APPLICATION ===");
        log.info("  Employee ID: {}, Year: {}, Month: {}", employeeId, year, month);
        log.info("  Excess Days (after carry forward check): {}", excessDays);

        // Calculate loss percentage (1% per day)
        double lossPercentage = excessDays * LOSS_OF_PAY_INCREMENT;

        log.info("  Loss Percentage: {} days × {}% = {}%",
                excessDays, LOSS_OF_PAY_INCREMENT, lossPercentage);

        // Create or update loss of pay record for this month
        LossOfPayRecord record =
                lossOfPayRecordRepository
                        .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                        .orElse(new LossOfPayRecord());

        record.setEmployeeId(employeeId);
        record.setYear(year);
        record.setMonth(month);
        record.setExcessDays(excessDays);
        record.setLossPercentage(lossPercentage);

        lossOfPayRecordRepository.save(record);

        // Get accumulated total loss of pay
        Double totalLoss = lossOfPayRecordRepository
                .getTotalLossPercentageByEmployeeIdAndYear(employeeId, year);

        log.info("  ✓ Loss of pay recorded for month {}: {}%", month, lossPercentage);
        log.info("  ✓ TOTAL ACCUMULATED LOSS OF PAY for year {}: {}%", year, totalLoss);
        log.info("  === LOSS OF PAY APPLICATION COMPLETE ===");
    }

    // =====================================================
    // ADAPTER METHOD: FOR APPROVAL FLOW INTEGRATION
    // =====================================================
    /**
     * Called from LeaveApprovalService when leave is approved
     * Automatically triggers monthly leave processing
     */
    @Transactional
    public void applyApprovedLeave(LeaveApplication leave) {

        // Safety check
        if (leave.getStatus() != LeaveStatus.APPROVED) {
            log.warn("Leave application {} is not approved, skipping processing", leave.getId());
            return;
        }

        Long employeeId = leave.getEmployeeId();
        Double approvedDays = leave.getDays().doubleValue();
        Integer year = leave.getStartDate().getYear();
        Integer month = leave.getStartDate().getMonthValue();

        log.info("Processing approved leave: application={}, employee={}, days={}, date={}-{}",
                leave.getId(), employeeId, approvedDays, year, month);

        // Delegate to monthly leave processing logic
        processMonthlyApprovedLeave(employeeId, approvedDays, year, month);
    }
}