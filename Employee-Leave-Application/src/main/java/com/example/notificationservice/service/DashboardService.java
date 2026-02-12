package com.example.notificationservice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.notificationservice.dto.EmployeeDashboardResponse;
import com.example.notificationservice.dto.response.MonthlyStatsResponse;
import com.example.notificationservice.entity.CarryForwardBalance;
import com.example.notificationservice.entity.CompOffBalance;
import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.entity.LeaveAllocation;
import com.example.notificationservice.repository.CarryForwardBalanceRepository;
import com.example.notificationservice.repository.CompOffBalanceRepository;
import com.example.notificationservice.repository.EmployeeRepository;
import com.example.notificationservice.repository.LeaveAllocationRepository;
import com.example.notificationservice.repository.LeaveApplicationRepository;
import com.example.notificationservice.repository.LossOfPayRecordRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final EmployeeRepository employeeRepo;
    private final LeaveAllocationRepository allocationRepo;
    private final LeaveApplicationRepository applicationRepo;
    private final CompOffBalanceRepository compOffRepo;
    private final CarryForwardBalanceRepository carryForwardRepo;
    private final LossOfPayRecordRepository lopRepo;

    /**
     * GET FULL EMPLOYEE DASHBOARD
     */
    @Transactional(readOnly = true)
    public EmployeeDashboardResponse getDashboard(Long employeeId) {

        log.info("[DASHBOARD] Fetching dashboard for employee: {}", employeeId);

        // Get employee
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        EmployeeDashboardResponse response = new EmployeeDashboardResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setCurrentYear(currentYear);
        response.setLastUpdated(LocalDateTime.now());

        // ═══════════════════════════════════════════════════════════
        // 1. Calculate YEARLY stats
        // ═══════════════════════════════════════════════════════════

        List<LeaveAllocation> allocations = allocationRepo
                .findByEmployeeIdAndYear(employeeId, currentYear);

        double yearlyAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        // Get total used (APPROVED only)
        Double yearlyUsed = applicationRepo.getTotalUsedDays(
                employeeId,
                com.example.notificationservice.enums.LeaveStatus.APPROVED,
                currentYear
        );
        if (yearlyUsed == null) yearlyUsed = 0.0;

        double yearlyBalance = yearlyAllocated - yearlyUsed;

        response.setYearlyAllocated(yearlyAllocated);
        response.setYearlyUsed(yearlyUsed);
        response.setYearlyBalance(yearlyBalance);

        // ═══════════════════════════════════════════════════════════
        // 2. Calculate MONTHLY stats
        // ═══════════════════════════════════════════════════════════

        response.setMonthlyAllocated(2.0); // Policy: 2 leaves per month

        // Count approved in current month
        int approvedThisMonth = applicationRepo.countApprovedInMonth(
                employeeId, currentYear, currentMonth);

        double monthlyUsed = (double) approvedThisMonth;
        response.setMonthlyUsed(monthlyUsed);
        response.setMonthlyBalance(2.0 - monthlyUsed);

        // ═══════════════════════════════════════════════════════════
        // 3. Carry Forward stats
        // ═══════════════════════════════════════════════════════════

        CarryForwardBalance cfBalance = carryForwardRepo
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .orElse(null);

        if (cfBalance != null) {
            response.setCarryForwardTotal(cfBalance.getTotalCarriedForward());
            response.setCarryForwardUsed(cfBalance.getTotalUsed());
            response.setCarryForwardRemaining(cfBalance.getRemaining());
        } else {
            response.setCarryForwardTotal(0.0);
            response.setCarryForwardUsed(0.0);
            response.setCarryForwardRemaining(0.0);
        }

        // ═══════════════════════════════════════════════════════════
        // 4. Comp-off balance
        // ═══════════════════════════════════════════════════════════

        List<CompOffBalance> compOffList = compOffRepo
                .findByEmployeeId(employeeId);

        if (!compOffList.isEmpty()) {
            response.setCompoffBalance(compOffList.get(0).getBalance());
        } else {
            response.setCompoffBalance(0.0);
        }

        // ═══════════════════════════════════════════════════════════
        // 5. Loss of Pay
        // ═══════════════════════════════════════════════════════════

        Double totalLOP = lopRepo.getTotalLossPercentageByEmployeeIdAndYear(employeeId, currentYear);
        response.setLossOfPayPercentage(totalLOP != null ? totalLOP : 0.0);

        log.info("[DASHBOARD] Dashboard fetched successfully for employee: {}", employeeId);

        return response;
    }

    /**
     * GET MONTHLY STATISTICS
     */
    @Transactional(readOnly = true)
    public MonthlyStatsResponse getMonthlyStats(Long employeeId, Integer year, Integer month) {

        log.info("[DASHBOARD] Fetching monthly stats: employee={}, year={}, month={}",
                employeeId, year, month);

        MonthlyStatsResponse response = new MonthlyStatsResponse();
        response.setEmployeeId(employeeId);
        response.setYear(year);
        response.setMonth(month);

        // Count approved leaves in this month
        int approvedCount = applicationRepo.countApprovedInMonth(employeeId, year, month);

        response.setTotalApprovedCount(approvedCount);
        response.setExceededLimit(approvedCount > 2);

        return response;
    }

    /**
     * Update dashboard (called after leave processing)
     */
    @Transactional
    public void refreshDashboard(Long employeeId, Integer year) {
        log.info("[DASHBOARD] Refreshing dashboard for employee: {} year: {}", employeeId, year);

        // This method can be used to trigger any dashboard updates
        // Currently, dashboard is computed on-demand in getDashboard()
        // If you want to cache dashboard data, implement caching logic here

        log.info("[DASHBOARD] Dashboard refresh completed");
    }
}