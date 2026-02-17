// ═══════════════════════════════════════════════════════════════════
// FILE: CompOffController.java
// Location: src/main/java/com/example/notificationservice/controller/
// ═══════════════════════════════════════════════════════════════════

package com.example.employeeLeaveApplication.controller;

import com.example.employeeLeaveApplication.entity.CompOffBalance;
import com.example.employeeLeaveApplication.service.CompOffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/comp-off")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CompOffController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CompOffController.class);

    private final CompOffService compOffService;

    /**
     * GET AVAILABLE COMP-OFF BALANCE
     * GET /api/comp-off/balance/{employeeId}
     */
    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long employeeId) {

        log.info("💰 [API] GET comp-off balance: employee={}", employeeId);

        try {
            BigDecimal balance = compOffService.getAvailableCompOffDays(employeeId);
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            log.error("❌ [API] Error getting balance: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET COMP-OFF BALANCE FOR SPECIFIC YEAR
     * GET /api/comp-off/balance/{employeeId}/year/{year}
     */
    @GetMapping("/balance/{employeeId}/year/{year}")
    public ResponseEntity<CompOffBalance> getBalanceForYear(
            @PathVariable Long employeeId,
            @PathVariable Integer year) {

        log.info("💰 [API] GET comp-off balance: employee={}, year={}", employeeId, year);

        try {
            CompOffBalance balance = compOffService.getBalanceForYear(employeeId, year);
            if (balance == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            log.error("❌ [API] Error getting balance: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * EARN COMP-OFF DAYS
     * POST /api/comp-off/earn?employeeId=1&year=2025&days=1.0
     */
    @PostMapping("/earn")
    public ResponseEntity<String> earnCompOff(
            @RequestParam Long employeeId,
            @RequestParam Integer year,
            @RequestParam Double days) {

        log.info("💰 [API] POST earn comp-off: employee={}, year={}, days={}",
                employeeId, year, days);

        try {
            compOffService.earnCompOff(employeeId, year, days);
            return ResponseEntity.ok("Earned " + days + " comp-off days successfully");
        } catch (Exception e) {
            log.error("❌ [API] Error earning comp-off: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * USE COMP-OFF DAYS (Manual - for testing)
     * POST /api/comp-off/use?employeeId=1&year=2025&days=0.5
     */
    @PostMapping("/use")
    public ResponseEntity<String> useCompOff(
            @RequestParam Long employeeId,
            @RequestParam Integer year,
            @RequestParam Double days) {

        log.info("📤 [API] POST use comp-off: employee={}, year={}, days={}",
                employeeId, year, days);

        try {
            double used = compOffService.useCompOff(employeeId, year, days);
            return ResponseEntity.ok("Used " + used + " comp-off days successfully");
        } catch (Exception e) {
            log.error("❌ [API] Error using comp-off: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * RESTORE COMP-OFF DAYS (When leave cancelled)
     * POST /api/comp-off/restore?employeeId=1&year=2025&days=0.5
     */
    @PostMapping("/restore")
    public ResponseEntity<String> restoreCompOff(
            @RequestParam Long employeeId,
            @RequestParam Integer year,
            @RequestParam Double days) {

        log.info("🔄 [API] POST restore comp-off: employee={}, year={}, days={}",
                employeeId, year, days);

        try {
            compOffService.restoreCompOff(employeeId, year, days);
            return ResponseEntity.ok("Restored " + days + " comp-off days successfully");
        } catch (Exception e) {
            log.error("❌ [API] Error restoring comp-off: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}