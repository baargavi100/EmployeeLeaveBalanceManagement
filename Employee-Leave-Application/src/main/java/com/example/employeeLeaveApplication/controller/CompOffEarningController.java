package com.example.employeeLeaveApplication.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.employeeLeaveApplication.dto.CompOffEarnRequestDTO;
import com.example.employeeLeaveApplication.entity.CompOff;
import com.example.employeeLeaveApplication.service.CompOffService;

import lombok.RequiredArgsConstructor;

/**
 * CompOff Management Controller
 * Handles earning requests, approvals, and history
 */
@RestController
@RequestMapping("/api/compoff")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CompOffEarningController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CompOffEarningController.class);

    private final CompOffService compOffService;

    // ═══════════════════════════════════════════════════════════════
    // REQUEST COMP-OFF (Employee initiates)
    // ═══════════════════════════════════════════════════════════════

    /**
     * REQUEST COMP-OFF
     * POST /api/compoff/request
     *
     * Employee submits comp-off request for working on holiday/weekend
     * Status: PENDING (awaiting manager approval)
     */
    @PostMapping("/request")
    public ResponseEntity<CompOff> requestCompOff(
            @RequestBody CompOffEarnRequestDTO request) {

        log.info("📝 [API] Requesting comp-off for employee: {}", request.getEmployeeId());

        try {
            if (!request.isValid()) {
                return ResponseEntity.badRequest().build();
            }

            CompOff compOff = compOffService.requestCompOff(
                    request.getEmployeeId(),
                    request.getWorkedDate(),
                    request.getDays(),
                    request.getDescription()
            );

            return ResponseEntity.ok(compOff);
        } catch (Exception e) {
            log.error("❌ Error requesting comp-off: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // APPROVE/REJECT COMP-OFF (Manager action)
    // ═══════════════════════════════════════════════════════════════

    /**
     * APPROVE COMP-OFF REQUEST
     * POST /api/compoff/{compOffId}/approve
     *
     * Manager approves pending comp-off request
     * Status: PENDING → EARNED
     * Balance gets updated
     */
    @PostMapping("/{compOffId}/approve")
    public ResponseEntity<CompOff> approveCompOff(
            @PathVariable Long compOffId) {

        log.info("✅ [API] Approving comp-off request: {}", compOffId);

        try {
            CompOff approved = compOffService.approveCompOffRequest(compOffId);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            log.error("❌ Error approving comp-off: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * REJECT COMP-OFF REQUEST
     * POST /api/compoff/{compOffId}/reject
     *
     * Manager rejects pending comp-off request
     * Status: PENDING → REJECTED
     */
    @PostMapping("/{compOffId}/reject")
    public ResponseEntity<String> rejectCompOff(
            @PathVariable Long compOffId) {

        log.info("❌ [API] Rejecting comp-off request: {}", compOffId);

        try {
            compOffService.rejectCompOffRequest(compOffId);
            return ResponseEntity.ok("Comp-off request rejected successfully");
        } catch (Exception e) {
            log.error("❌ Error rejecting comp-off: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET PENDING APPROVALS (For Manager)
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET PENDING COMP-OFF APPROVALS
     * GET /api/compoff/pending/{managerId}
     *
     * Returns all pending comp-off requests awaiting manager approval
     */
    @GetMapping("/pending/{managerId}")
    public ResponseEntity<List<CompOff>> getPendingApprovals(
            @PathVariable Long managerId) {

        log.info("📋 [API] Getting pending comp-off approvals for manager: {}", managerId);

        try {
            List<CompOff> pending = compOffService.getPendingApprovals(managerId);
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            log.error("❌ Error getting pending approvals: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
