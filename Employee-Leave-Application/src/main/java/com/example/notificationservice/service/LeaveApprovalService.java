// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveApprovalService.java (ENHANCED - With better error handling)
// Location: src/main/java/com/example/notificationservice/service/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.notificationservice.constants.PolicyConstants;
import com.example.notificationservice.dto.CarryForwardBalanceResponse;
import com.example.notificationservice.dto.LeaveApprovalSimulationResponse;
import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.enums.LeaveStatus;
import com.example.notificationservice.enums.LeaveType;
import com.example.notificationservice.enums.Role;
import com.example.notificationservice.repository.LeaveApplicationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveApprovalService {
    
    private static final Logger log = LoggerFactory.getLogger(LeaveApprovalService.class);

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final CarryForwardService carryForwardService;
    private final CompOffService compOffService;
    private final LossOfPayService lossOfPayService;

    // ═══════════════════════════════════════════════════════════════
    // SIMULATE APPROVAL (Preview what will happen)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Simulate approval - shows what will happen before actual approval
     * Returns options if monthly limit exceeded
     */
    @Transactional(readOnly = true)
    public LeaveApprovalSimulationResponse simulateApproval(Long leaveId) {

        log.info("🎭 [SIMULATE] Simulating approval for leave: {}", leaveId);

        LeaveApplication leave = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave application not found: " + leaveId));

        Long employeeId = leave.getEmployeeId();
        Integer year = leave.getYear();
        Integer month = leave.getStartDate().getMonthValue();
        Double requestedDays = leave.getDays().doubleValue();
        LeaveType leaveType = leave.getLeaveType();

        LeaveApprovalSimulationResponse response = new LeaveApprovalSimulationResponse();
        response.setLeaveId(leaveId);
        response.setEmployeeId(employeeId);
        response.setRequestedDays(requestedDays);
        response.setLeaveType(leaveType);

        // ═══════════════════════════════════════════════════════════
        // SPECIAL CASE: COMP_OFF - No monthly limit, direct deduction
        // ═══════════════════════════════════════════════════════════

        if (leaveType == LeaveType.COMP_OFF) {

            log.info("   COMP_OFF leave - checking balance");

            BigDecimal compOffBalanceBD = compOffService.getAvailableCompOffDays(employeeId);
            Double compOffBalance = compOffBalanceBD.doubleValue();

            if (compOffBalance >= requestedDays) {
                response.setCanApproveDirectly(true);
                response.setMessage("COMP_OFF leave can be approved directly");
                response.setRequiresUserDecision(false);
            } else {
                response.setCanApproveDirectly(false);
                response.setMessage("Insufficient COMP_OFF balance. Available: " + compOffBalance + ", Requested: " + requestedDays);
                response.setRequiresUserDecision(false);
            }

            return response;
        }

        // ═══════════════════════════════════════════════════════════
        // REGULAR LEAVE TYPES: Check monthly limit
        // ═══════════════════════════════════════════════════════════

        // Get approved days in this month (excluding current leave)
        Double approvedInMonth = leaveApplicationRepository
                .getTotalApprovedDaysInMonth(employeeId, year, month);

        if (approvedInMonth == null) approvedInMonth = 0.0;

        double totalAfterApproval = approvedInMonth + requestedDays;

        log.info("   Approved in month: {}, Requested: {}, Total: {}, Limit: {}",
                approvedInMonth, requestedDays, totalAfterApproval, PolicyConstants.MONTHLY_LIMIT);

        // ═══════════════════════════════════════════════════════════
        // CASE 1: Within monthly limit - Approve directly
        // ═══════════════════════════════════════════════════════════

        if (totalAfterApproval <= PolicyConstants.MONTHLY_LIMIT) {
            response.setCanApproveDirectly(true);
            response.setMessage("Within monthly limit. Can approve directly.");
            response.setRequiresUserDecision(false);
            return response;
        }


        // ═══════════════════════════════════════════════════════════
        // CASE 2: Exceeds monthly limit - Check alternatives
        // ═══════════════════════════════════════════════════════════

        double excessDays = totalAfterApproval - PolicyConstants.MONTHLY_LIMIT;

        log.info("   Exceeds monthly limit by {} days", excessDays);

        response.setCanApproveDirectly(false);
        response.setExcessDays(excessDays);

        // Check Carry Forward availability
        CarryForwardBalanceResponse cfBalance = carryForwardService.getBalance(employeeId, year);
        Double carryForwardAvailable = 0.0;
        if (cfBalance != null && cfBalance.getRemaining() != null) {
            carryForwardAvailable = cfBalance.getRemaining();
        }

        if (carryForwardAvailable >= excessDays) {
            // Can use carry forward - auto-approve
            response.setCanApproveDirectly(true);
            response.setCanUseCarryForward(true);
            response.setCarryForwardAvailable(carryForwardAvailable);
            response.setMessage("Exceeds monthly limit by " + excessDays + " days. Will use carry forward automatically.");
            response.setRequiresUserDecision(false);
            return response;
        }

        // Carry forward insufficient - offer choices
        response.setCanUseCarryForward(false);
        response.setCarryForwardAvailable(carryForwardAvailable);

        // Check CompOff availability
        BigDecimal compOffBD = compOffService.getAvailableCompOffDays(employeeId);
        Double compOffAvailable = compOffBD.doubleValue();
        response.setCompOffAvailable(compOffAvailable);
        response.setCanUseCompOff(compOffAvailable >= excessDays);

        // Calculate LOP
        double lopPercentage = excessDays * PolicyConstants.LOSS_OF_PAY_PERCENT_PER_DAY;
        response.setLopPercentage(lopPercentage);

        response.setRequiresUserDecision(true);
        response.setMessage("Exceeds monthly limit by " + excessDays + " days. " +
                "Carry forward: " + carryForwardAvailable + " days (insufficient). " +
                "Choose: Use CompOff (" + compOffAvailable + " available) OR Accept " + lopPercentage + "% LOP.");

        log.info("✅ [SIMULATE] Simulation complete - requires user decision");

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // APPROVE LEAVE (Actual approval with user decision)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void approveLeave(Long leaveId, Long approverId, Role approverRole,
                             Boolean useCompOff, Boolean allowLOP) {

        log.info("✅ [APPROVE] Approving leave: {}, approver: {}, useCompOff: {}, allowLOP: {}",
                leaveId, approverId, useCompOff, allowLOP);

        LeaveApplication leave = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave application not found: " + leaveId));

        // First simulate to get deduction details
        LeaveApprovalSimulationResponse simulation = simulateApproval(leaveId);

        // Handle different scenarios
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            // COMP_OFF: Direct deduction from comp-off balance
            handleCompOffLeave(leave, simulation);

        } else if (simulation.getCanApproveDirectly() && !simulation.getRequiresUserDecision()) {
            // Within limit OR carry forward auto-used
            handleDirectApproval(leave, simulation);

        } else if (simulation.getRequiresUserDecision()) {
            // Exceeds limit, carry forward insufficient - user must decide
            handleExcessDays(leave, simulation, useCompOff, allowLOP);
        }

        // Update leave status
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(approverId);
        leave.setApprovedRole(approverRole);
        leave.setApprovedAt(LocalDateTime.now());

        leaveApplicationRepository.save(leave);

        log.info("✅ [APPROVE] Leave approved successfully");
    }

    private void handleCompOffLeave(LeaveApplication leave, LeaveApprovalSimulationResponse simulation) {

        if (!simulation.getCanApproveDirectly()) {
            throw new RuntimeException("Insufficient COMP_OFF balance. " + simulation.getMessage());
        }

        log.info("   Deducting COMP_OFF: {} days", leave.getDays().doubleValue());
        compOffService.useCompOff(leave.getEmployeeId(), leave.getYear(), leave.getDays().doubleValue());
        leave.setCompOffUsed(leave.getDays().doubleValue());
    }

    private void handleDirectApproval(LeaveApplication leave, LeaveApprovalSimulationResponse simulation) {

        // Check if carry forward was used
        if (simulation.getCanUseCarryForward() != null && simulation.getCanUseCarryForward()
                && simulation.getExcessDays() != null && simulation.getExcessDays() > 0) {

            // Auto-use carry forward for excess
            log.info("   Auto-using carry forward: {} days", simulation.getExcessDays());
            double used = carryForwardService.useCarryForward(
                    leave.getEmployeeId(), leave.getYear(), simulation.getExcessDays());
            leave.setCarryForwardUsed(used);
        }
    }

    private void handleExcessDays(LeaveApplication leave, LeaveApprovalSimulationResponse simulation,
                                  Boolean useCompOff, Boolean allowLOP) {

        Double excessDays = simulation.getExcessDays();

        if (excessDays == null || excessDays <= 0) {
            return; // No excess to handle
        }

        Integer year = leave.getYear();
        Integer month = leave.getStartDate().getMonthValue();
        Long employeeId = leave.getEmployeeId();

        // ═══════════════════════════════════════════════════════════
        // USER MUST CHOOSE: CompOff OR LOP
        // ═══════════════════════════════════════════════════════════

        if (useCompOff != null && useCompOff) {

            // OPTION 1: Use CompOff
            if (!simulation.getCanUseCompOff()) {
                throw new RuntimeException(
                        "Insufficient CompOff balance. Available: " +
                                simulation.getCompOffAvailable() + ", Required: " + excessDays);
            }

            log.info("   User chose: Use CompOff for {} excess days", excessDays);
            compOffService.useCompOff(employeeId, year, excessDays);
            leave.setCompOffUsed(excessDays);
            leave.setLossOfPayApplied(0.0); // No LOP

        } else if (allowLOP != null && allowLOP) {

            // OPTION 2: Accept Loss of Pay
            log.info("   User chose: Accept LOP for {} excess days", excessDays);

            // Calculate LOP percentage
            double lopPercent = excessDays * PolicyConstants.LOSS_OF_PAY_PERCENT_PER_DAY;

            // Store in leave application
            leave.setLossOfPayApplied(lopPercent);
            leave.setCompOffUsed(0.0); // No CompOff used

            // ✅ RECORD IN LOSS_OF_PAY_RECORD TABLE (for monthly tracking)
            lossOfPayService.applyLossOfPay(employeeId, year, month, excessDays);

            log.info("   LOP applied: {}% recorded in loss_of_pay_record table", lopPercent);

        } else {
            // User must make a choice!
            throw new RuntimeException(
                    "Monthly limit exceeded by " + excessDays + " days. " +
                            "You must choose: useCompOff=true OR allowLOP=true");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REJECT LEAVE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void rejectLeave(Long leaveId, Long approverId, Role approverRole) {

        log.info("❌ [REJECT] Rejecting leave: {}, approver: {}", leaveId, approverId);

        LeaveApplication leave = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave application not found: " + leaveId));

        // Restore any deductions if rejecting an already-applied leave
        // (edge case: if leave was partially processed)
        if (leave.getStatus() == LeaveStatus.APPROVED) {
            // Restore CompOff if used
            if (leave.getCompOffUsed() != null && leave.getCompOffUsed() > 0) {
                log.info("   Restoring CompOff on rejection: {} days", leave.getCompOffUsed());
                compOffService.restoreCompOff(leave.getEmployeeId(), leave.getYear(), leave.getCompOffUsed());
            }

            // Restore Carry Forward if used
            if (leave.getCarryForwardUsed() != null && leave.getCarryForwardUsed() > 0) {
                log.info("   Restoring Carry Forward on rejection: {} days", leave.getCarryForwardUsed());
                carryForwardService.restoreCarryForward(leave.getEmployeeId(), leave.getYear(), leave.getCarryForwardUsed());
            }

            // Restore LOP if applied
            if (leave.getLossOfPayApplied() != null && leave.getLossOfPayApplied() > 0) {
                log.info("   Restoring LOP on rejection: {}%", leave.getLossOfPayApplied());
                lossOfPayService.restoreLossOfPay(
                        leave.getEmployeeId(),
                        leave.getYear(),
                        leave.getStartDate().getMonthValue()
                );
            }
        }

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApprovedBy(approverId);
        leave.setApprovedRole(approverRole);
        leave.setApprovedAt(LocalDateTime.now());

        leaveApplicationRepository.save(leave);

        log.info("✅ [REJECT] Leave rejected successfully with restoration if applicable");
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCEL LEAVE (With restoration)
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public void cancelLeave(Long leaveId) {

        log.info("🔄 [CANCEL] Cancelling leave: {}", leaveId);

        LeaveApplication leave = leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave application not found: " + leaveId));

        // Only restore if leave was APPROVED
        if (leave.getStatus() != LeaveStatus.APPROVED) {
            throw new RuntimeException("Can only cancel APPROVED leaves. Current status: " + leave.getStatus());
        }

        // Restore CompOff if used
        if (leave.getCompOffUsed() != null && leave.getCompOffUsed() > 0) {
            log.info("   Restoring CompOff: {} days", leave.getCompOffUsed());
            compOffService.restoreCompOff(leave.getEmployeeId(), leave.getYear(), leave.getCompOffUsed());
        }

        // Restore Carry Forward if used
        if (leave.getCarryForwardUsed() != null && leave.getCarryForwardUsed() > 0) {
            log.info("   Restoring Carry Forward: {} days", leave.getCarryForwardUsed());
            carryForwardService.restoreCarryForward(leave.getEmployeeId(), leave.getYear(), leave.getCarryForwardUsed());
        }

        // ✅ RESTORE LOP RECORD (delete the monthly LOP entry)
        if (leave.getLossOfPayApplied() != null && leave.getLossOfPayApplied() > 0) {
            log.info("   Restoring LOP: Removing {}% from month {}",
                    leave.getLossOfPayApplied(), leave.getStartDate().getMonthValue());

            // Delete LOP record for that month
            lossOfPayService.restoreLossOfPay(
                    leave.getEmployeeId(),
                    leave.getYear(),
                    leave.getStartDate().getMonthValue()
            );
        }

        // Clear deduction fields
        leave.setCompOffUsed(0.0);
        leave.setCarryForwardUsed(0.0);
        leave.setLossOfPayApplied(0.0);

        // Update status
        leave.setStatus(LeaveStatus.CANCELLED);
        leaveApplicationRepository.save(leave);

        log.info("✅ [CANCEL] Leave cancelled successfully with full restoration");
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY: Find leave by employee and status
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public LeaveApplication findById(Long leaveId) {
        return leaveApplicationRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave application not found: " + leaveId));
    }
}