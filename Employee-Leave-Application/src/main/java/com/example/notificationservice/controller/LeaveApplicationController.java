// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveApplicationController.java (FIXED)
// Location: src/main/java/com/example/notificationservice/controller/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.controller;

import com.example.notificationservice.dto.LeaveApprovalSimulationResponse;
import com.example.notificationservice.entity.LeaveApplication;
import com.example.notificationservice.enums.LeaveStatus;
import com.example.notificationservice.enums.Role;
import com.example.notificationservice.repository.LeaveApplicationRepository;
import com.example.notificationservice.service.LeaveApprovalService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-application")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LeaveApplicationController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LeaveApplicationController.class);

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveApprovalService leaveApprovalService;

    // ═══════════════════════════════════════════════════════════════
    // SIMULATE APPROVAL (Preview)
    // ═══════════════════════════════════════════════════════════════

    /**
     * SIMULATE APPROVAL
     * GET /api/leave-application/{leaveId}/simulate
     *
     * Shows what will happen before actual approval
     */
    @GetMapping("/{leaveId}/simulate")
    public ResponseEntity<?> simulateApproval(@PathVariable Long leaveId) {

        log.info("[SIMULATE] Simulating approval for leave: {}", leaveId);

        try {
            LeaveApprovalSimulationResponse simulation =
                    leaveApprovalService.simulateApproval(leaveId);

            return ResponseEntity.ok(simulation);

        } catch (Exception e) {
            log.error("[SIMULATE] Error simulating approval: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // APPROVE LEAVE
    // ═══════════════════════════════════════════════════════════════

    /**
     * APPROVE LEAVE
     * POST /api/leave-application/{leaveId}/approve?approverId=1&approverRole=MANAGER&useCompOff=true&allowLOP=false
     */
    @PostMapping("/{leaveId}/approve")
    public ResponseEntity<?> approveLeave(
            @PathVariable Long leaveId,
            @RequestParam Long approverId,
            @RequestParam Role approverRole,
            @RequestParam(required = false) Boolean useCompOff,
            @RequestParam(required = false) Boolean allowLOP) {

        log.info("[APPROVE] Approving leave: {}", leaveId);

        try {
            leaveApprovalService.approveLeave(leaveId, approverId, approverRole, useCompOff, allowLOP);
            return ResponseEntity.ok("Leave approved successfully");

        } catch (Exception e) {
            log.error("[APPROVE] Error approving leave: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REJECT LEAVE
    // ═══════════════════════════════════════════════════════════════

    /**
     * REJECT LEAVE
     * POST /api/leave-application/{leaveId}/reject?approverId=1&approverRole=MANAGER
     */
    @PostMapping("/{leaveId}/reject")
    public ResponseEntity<?> rejectLeave(
            @PathVariable Long leaveId,
            @RequestParam Long approverId,
            @RequestParam Role approverRole) {

        log.info("[REJECT] Rejecting leave: {}", leaveId);

        try {
            leaveApprovalService.rejectLeave(leaveId, approverId, approverRole);
            return ResponseEntity.ok("Leave rejected successfully");

        } catch (Exception e) {
            log.error("[REJECT] Error rejecting leave: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCEL LEAVE
    // ═══════════════════════════════════════════════════════════════

    /**
     * CANCEL LEAVE
     * POST /api/leave-application/{leaveId}/cancel
     */
    @PostMapping("/{leaveId}/cancel")
    public ResponseEntity<?> cancelLeave(@PathVariable Long leaveId) {

        log.info("[CANCEL] Cancelling leave: {}", leaveId);

        try {
            leaveApprovalService.cancelLeave(leaveId);
            return ResponseEntity.ok("Leave cancelled successfully");

        } catch (Exception e) {
            log.error("[CANCEL] Error cancelling leave: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERY METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET BY EMPLOYEE ID
     * GET /api/leave-application/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveApplication>> findByEmployeeId(
            @PathVariable Long employeeId) {

        List<LeaveApplication> applications =
                leaveApplicationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        return ResponseEntity.ok(applications);
    }

    /**
     * GET BY STATUS
     * GET /api/leave-application/status/{status}?employeeId=2
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<LeaveApplication>> findByStatus(
            @PathVariable LeaveStatus status,
            @RequestParam(required = false) Long employeeId) {

        if (employeeId != null) {
            List<LeaveApplication> applications =
                    leaveApplicationRepository.findByEmployeeIdAndStatus(employeeId, status);
            return ResponseEntity.ok(applications);
        } else {
            // Find all by status (for admin/HR)
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * GET PENDING LEAVES FOR MANAGER
     * GET /api/leave-application/manager/{managerId}/pending
     */
    @GetMapping("/manager/{managerId}/pending")
    public ResponseEntity<List<LeaveApplication>> findPendingLeavesForManager(
            @PathVariable Long managerId) {

        List<LeaveApplication> pending =
                leaveApplicationRepository.findPendingTeamRequests(managerId);
        return ResponseEntity.ok(pending);
    }
}