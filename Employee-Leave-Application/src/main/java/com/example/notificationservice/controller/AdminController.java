package com.example.notificationservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.notificationservice.service.CarryForwardService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final CarryForwardService carryForwardService;

    public AdminController(CarryForwardService carryForwardService){
        this.carryForwardService=carryForwardService;
    }

    @PostMapping("/carry-forward")
    public ResponseEntity<String> processCarryForward(
            @RequestParam Integer fromYear) {

        log.info("[API] POST carry-forward: fromYear={}", fromYear);

        try {
            carryForwardService.processYearEndCarryForward(fromYear);
            return ResponseEntity.ok(
                    "Carry forward processed successfully for year " + fromYear);
        } catch (Exception e) {
            log.error("[API] Carry forward failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Carry forward failed: " + e.getMessage());
        }
    }

    @PostMapping("/carry-forward/{employeeId}")
    public ResponseEntity<String> processEmployeeCarryForward(
            @PathVariable Long employeeId,
            @RequestParam Integer fromYear) {

        log.info("[API] POST carry-forward: employee={}, fromYear={}", employeeId, fromYear);

        try {
            carryForwardService.processEmployeeCarryForward(employeeId, fromYear);
            return ResponseEntity.ok(
                    "Carry forward processed for employee " + employeeId);
        } catch (Exception e) {
            log.error("[API] Carry forward failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Carry forward failed: " + e.getMessage());
        }
    }
}
