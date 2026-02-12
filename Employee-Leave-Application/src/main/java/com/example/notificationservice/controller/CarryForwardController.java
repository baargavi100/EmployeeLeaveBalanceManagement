// ═══════════════════════════════════════════════════════════════════
// CarryForwardController.java - For Employee/Manager to VIEW carry forward
// Location: src/main/java/com/example/notificationservice/controller/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.controller;

import com.example.notificationservice.dto.CarryForwardBalanceResponse;
import com.example.notificationservice.dto.CarryForwardEligibilityResponse;
import com.example.notificationservice.service.CarryForwardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carry-forward")
@CrossOrigin(origins = "*")
@Slf4j
public class CarryForwardController {

    private final CarryForwardService carryForwardService;

    public CarryForwardController(CarryForwardService carryForwardService) {
        this.carryForwardService = carryForwardService;
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * GET CARRY FORWARD BALANCE
     * GET /api/carry-forward/balance/{employeeId}?year=2026
     * ═══════════════════════════════════════════════════════════════
     */
    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<?> getCarryForwardBalance(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {

        log.info("[API] GET carry-forward balance: employee={}, year={}", employeeId, year);

        try {
            CarryForwardBalanceResponse response = carryForwardService.getBalance(employeeId, year);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[API] Failed to get balance: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * CHECK ELIGIBILITY FOR CARRY FORWARD
     * GET /api/carry-forward/eligibility/{employeeId}?year=2026
     * ═══════════════════════════════════════════════════════════════
     */
    @GetMapping("/eligibility/{employeeId}")
    public ResponseEntity<?> checkEligibility(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {

        log.info("[API] GET eligibility: employee={}, year={}", employeeId, year);

        try {
            CarryForwardEligibilityResponse response =
                    carryForwardService.checkEligibility(employeeId, year);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[API] Failed to check eligibility: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * USE CARRY FORWARD (Manual deduction for testing)
     * POST /api/carry-forward/use
     * ═══════════════════════════════════════════════════════════════
     */
    @PostMapping("/use")
    public ResponseEntity<?> useCarryForward(
            @RequestParam Long employeeId,
            @RequestParam Integer year,
            @RequestParam Double days) {

        log.info("[API] POST use carry-forward: employee={}, year={}, days={}",
                employeeId, year, days);

        try {
            carryForwardService.useCarryForward(employeeId, year, days);
            return ResponseEntity.ok("Used " + days + " days from carry forward successfully");
        } catch (Exception e) {
            log.error("[API] Failed to use carry forward: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * GET ALL EMPLOYEES WITH CARRY FORWARD (For HR)
     * GET /api/carry-forward/all?year=2026
     * ═══════════════════════════════════════════════════════════════
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllCarryForward(@RequestParam Integer year) {

        log.info("[API] GET all carry-forward: year={}", year);

        try {
            var allBalances = carryForwardService.getAllBalances(year);
            return ResponseEntity.ok(allBalances);
        } catch (Exception e) {
            log.error("[API] Failed to get all balances: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Error: " + e.getMessage());
        }
    }
}