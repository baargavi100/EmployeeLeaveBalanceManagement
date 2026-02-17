// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveApprovalController.java (FIXED - NO ERRORS)
// Location: src/main/java/com/example/notificationservice/controller/
// ═══════════════════════════════════════════════════════════════════

package com.example.employeeLeaveApplication.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.employeeLeaveApplication.dto.LeaveApprovalSimulationResponse;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.service.LeaveApprovalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/leave-approval")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LeaveApprovalController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LeaveApprovalController.class);

    private final LeaveApprovalService leaveApprovalService;
    private final LeaveApplicationRepository leaveApplicationRepository;

    // ═══════════════════════════════════════════════════════════════
    // SIMULATE APPROVAL (Preview before approving)
    // ═══════════════════════════════════════════════════════════════

    /**
     * SIMULATE APPROVAL
     * GET /api/leave-approval/{leaveId}/simulate
     *
     * Shows what will happen before actual approval
     * Returns: simulation with options if monthly limit exceeded
     */
    @GetMapping("/{leaveId}/simulate")
    public ResponseEntity<?> simulateApproval(@PathVariable Long leaveId) {

        log.info("[SIMULATE] Simulating approval for leave: {}", leaveId);

        try {
            LeaveApprovalSimulationResponse simulation =
                    leaveApprovalService.simulateApproval(leaveId);
            return ResponseEntity.ok(simulation);

        } catch (Exception e) {
            log.error("[SIMULATE] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // APPROVE LEAVE
    // ═══════════════════════════════════════════════════════════════

    /**
     * APPROVE LEAVE
     * POST /api/leave-approval/{leaveId}/approve
     *
     * Query params:
     * - approverId (required)
     * - approverRole (required): MANAGER, HR, ADMIN
     * - useCompOff (optional): true/false
     * - allowLOP (optional): true/false
     *
     * Example:
     * POST /api/leave-approval/1/approve?approverId=1&approverRole=MANAGER&useCompOff=true
     */
    @PostMapping("/{leaveId}/approve")
    public ResponseEntity<?> approveLeave(
            @PathVariable Long leaveId,
            @RequestParam Long approverId,
            @RequestParam Role approverRole,
            @RequestParam(required = false) Boolean useCompOff,
            @RequestParam(required = false) Boolean allowLOP) {

        log.info("[APPROVE] Approving leave: {}, approver: {}", leaveId, approverId);

        try {
            leaveApprovalService.approveLeave(leaveId, approverId, approverRole, useCompOff, allowLOP);
            return ResponseEntity.ok("Leave approved successfully");

        } catch (Exception e) {
            log.error("[APPROVE] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REJECT LEAVE
    // ═══════════════════════════════════════════════════════════════

    /**
     * REJECT LEAVE
     * POST /api/leave-approval/{leaveId}/reject
     *
     * Query params:
     * - approverId (required)
     * - approverRole (required)
     *
     * Example:
     * POST /api/leave-approval/1/reject?approverId=1&approverRole=MANAGER
     */
    @PostMapping("/{leaveId}/reject")
    public ResponseEntity<?> rejectLeave(
            @PathVariable Long leaveId,
            @RequestParam Long approverId,
            @RequestParam Role approverRole) {

        log.info("[REJECT] Rejecting leave: {}, approver: {}", leaveId, approverId);

        try {
            leaveApprovalService.rejectLeave(leaveId, approverId, approverRole);
            return ResponseEntity.ok("Leave rejected successfully");

        } catch (Exception e) {
            log.error("[REJECT] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCEL LEAVE
    // ═══════════════════════════════════════════════════════════════

    /**
     * CANCEL LEAVE
     * POST /api/leave-approval/{leaveId}/cancel
     *
     * Cancels an approved leave and restores balances
     */
    @PostMapping("/{leaveId}/cancel")
    public ResponseEntity<?> cancelLeave(@PathVariable Long leaveId) {

        log.info("[CANCEL] Cancelling leave: {}", leaveId);

        try {
            leaveApprovalService.cancelLeave(leaveId);
            return ResponseEntity.ok("Leave cancelled successfully");

        } catch (Exception e) {
            log.error("[CANCEL] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET PENDING LEAVES FOR MANAGER
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET PENDING LEAVES FOR MANAGER
     * GET /api/leave-approval/manager/{managerId}/pending
     *
     * Returns list of pending leave requests for manager's team
     */
    @GetMapping("/manager/{managerId}/pending")
    public ResponseEntity<List<LeaveApplication>> getPendingLeaves(
            @PathVariable Long managerId) {

        log.info("[APPROVAL] Fetching pending leaves for manager: {}", managerId);

        try {
            // Use repository method directly
            List<LeaveApplication> leaves =
                    leaveApplicationRepository.getPendingLeavesForManager(managerId);

            return ResponseEntity.ok(leaves);

        } catch (Exception e) {
            log.error("[APPROVAL] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET LEAVE BY ID
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET LEAVE APPLICATION BY ID
     * GET /api/leave-approval/{leaveId}
     */
    @GetMapping("/{leaveId}")
    public ResponseEntity<?> getLeaveById(@PathVariable Long leaveId) {

        log.info("[APPROVAL] Fetching leave: {}", leaveId);

        try {
            LeaveApplication leave = leaveApprovalService.findById(leaveId);
            return ResponseEntity.ok(leave);

        } catch (Exception e) {
            log.error("[APPROVAL] Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}