// ═══════════════════════════════════════════════════════════════════
// FILE: CarryForwardController.java
// Location: src/main/java/com/example/notificationservice/controller/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.controller;

import com.example.notificationservice.dto.CarryForwardBalanceResponse;
import com.example.notificationservice.dto.CarryForwardEligibilityResponse;
import com.example.notificationservice.service.CarryForwardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carryforward")
@RequiredArgsConstructor
@Slf4j
public class CarryForwardController {

    private final CarryForwardService carryForwardService;

    // ═══════════════════════════════════════════════════════════════
    // GET CARRY FORWARD BALANCE
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<CarryForwardBalanceResponse> getBalance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {

        log.info("[CARRYFORWARD] Fetching balance: employee={}, year={}", employeeId, year);

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        CarryForwardBalanceResponse balance = carryForwardService.getBalance(employeeId, year);

        return ResponseEntity.ok(balance);
    }

    // ═══════════════════════════════════════════════════════════════
    // CHECK ELIGIBILITY
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/eligibility/{employeeId}")
    public ResponseEntity<CarryForwardEligibilityResponse> checkEligibility(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {

        log.info("[CARRYFORWARD] Checking eligibility: employee={}, year={}", employeeId, year);

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        CarryForwardEligibilityResponse eligibility =
                carryForwardService.checkEligibility(employeeId, year);

        return ResponseEntity.ok(eligibility);
    }

    // ═══════════════════════════════════════════════════════════════
    // PROCESS YEAR-END CARRY FORWARD (HR/Admin only)
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/process/{year}")
    public ResponseEntity<?> processYearEnd(@PathVariable Integer year) {

        log.info("[CARRYFORWARD] Processing year-end carry forward for year: {}", year);

        try {
            carryForwardService.processYearEndCarryForward(year);

            return ResponseEntity.ok(Map.of(
                    "message", "Year-end carry forward processed successfully",
                    "year", year
            ));

        } catch (Exception e) {
            log.error("[CARRYFORWARD] Error processing year-end", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process year-end carry forward",
                    "details", e.getMessage()
            ));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET ALL BALANCES FOR A YEAR (HR Report)
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/balances/{year}")
    public ResponseEntity<List<CarryForwardBalanceResponse>> getAllBalances(
            @PathVariable Integer year) {

        log.info("[CARRYFORWARD] Fetching all balances for year: {}", year);

        List<CarryForwardBalanceResponse> balances =
                carryForwardService.getAllBalances(year);

        return ResponseEntity.ok(balances);
    }
}