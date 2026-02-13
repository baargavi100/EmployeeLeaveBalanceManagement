// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveBalanceController.java
// Location: src/main/java/com/example/notificationservice/controller/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.controller;

import com.example.notificationservice.dto.LeaveBalanceResponse;
import com.example.notificationservice.enums.LeaveType;
import com.example.notificationservice.service.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave-balance")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    /**
     * GET LEAVE BALANCE
     * GET /api/leave-balance/{employeeId}?year=2025
     */
    @GetMapping("/{employeeId}")
    public ResponseEntity<LeaveBalanceResponse> getBalance(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {

        log.info("📊 [API] GET balance: employee={}, year={}", employeeId, year);

        try {
            LeaveBalanceResponse response = leaveBalanceService.getBalance(employeeId, year);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [API] Error getting balance: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * INITIALIZE ALLOCATIONS FOR NEW EMPLOYEE
     * POST /api/leave-balance/initialize?employeeId=1&year=2025
     */
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeAllocations(
            @RequestParam Long employeeId,
            @RequestParam Integer year) {

        log.info("🆕 [API] POST initialize: employee={}, year={}", employeeId, year);

        try {
            leaveBalanceService.initializeAllocations(employeeId, year);
            return ResponseEntity.ok("Allocations initialized successfully for employee: " + employeeId);
        } catch (Exception e) {
            log.error("❌ [API] Error initializing: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * CHECK SUFFICIENT BALANCE
     * GET /api/leave-balance/check-balance?employeeId=1&year=2025&leaveType=VACATION&days=2
     */
    @GetMapping("/check-balance")
    public ResponseEntity<Boolean> checkBalance(
            @RequestParam Long employeeId,
            @RequestParam Integer year,
            @RequestParam LeaveType leaveType,
            @RequestParam Double days) {

        log.info("🔍 [API] GET check-balance: employee={}, type={}, days={}",
                employeeId, leaveType, days);

        try {
            boolean hasSufficient = leaveBalanceService.hasSufficientBalance(
                    employeeId, year, leaveType, days);
            return ResponseEntity.ok(hasSufficient);
        } catch (Exception e) {
            log.error("❌ [API] Error checking balance: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET TOTAL LOSS OF PAY
     * GET /api/leave-balance/lop/{employeeId}?year=2025
     */
    @GetMapping("/lop/{employeeId}")
    public ResponseEntity<Double> getLossOfPay(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {

        log.info("💰 [API] GET LOP: employee={}, year={}", employeeId, year);

        try {
            Double lop = leaveBalanceService.getTotalLossOfPayPercentage(employeeId, year);
            return ResponseEntity.ok(lop);
        } catch (Exception e) {
            log.error("❌ [API] Error getting LOP: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}