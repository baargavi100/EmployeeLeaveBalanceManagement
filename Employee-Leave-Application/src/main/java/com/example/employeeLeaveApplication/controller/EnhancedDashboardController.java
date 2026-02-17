package com.example.employeeLeaveApplication.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.employeeLeaveApplication.component.CarryForwardScheduler;
import com.example.employeeLeaveApplication.dto.AdminDashboardResponse;
import com.example.employeeLeaveApplication.dto.HRDashboardResponse;
import com.example.employeeLeaveApplication.dto.ManagerDashboardResponse;
import com.example.employeeLeaveApplication.service.DashboardService;

import lombok.RequiredArgsConstructor;

/**
 * Enhanced Dashboard Controller
 * Consolidated endpoints for Employee, Manager, HR, and Admin dashboards
 */
@RestController
@RequestMapping("/api/dashboard/v2")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EnhancedDashboardController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EnhancedDashboardController.class);

    private final DashboardService dashboardService;
    private final CarryForwardScheduler carryForwardScheduler;

    // ═══════════════════════════════════════════════════════════════
    // MANAGER DASHBOARD ENDPOINTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET MANAGER DASHBOARD
     * GET /api/dashboard/v2/manager/{managerId}
     *
     * Returns:
     * - Manager's own stats (yearly, monthly, CF, CompOff, LOP)
     * - Team size & pending requests count
     * - Detailed pending team requests
     * - Team members on leave today
     */
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<ManagerDashboardResponse> getManagerDashboard(
            @PathVariable Long managerId) {

        log.info("👔 [API] GET manager dashboard: {}", managerId);

        try {
            ManagerDashboardResponse response = dashboardService.getManagerDashboard(managerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error getting manager dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HR DASHBOARD ENDPOINTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET HR DASHBOARD
     * GET /api/dashboard/v2/hr
     *
     * Returns:
     * - Company-wide metrics (total employees, on leave, pending)
     * - Onboarding status (pending biometric, VPN)
     * - Employees currently on leave (with manager info)
     * - Manager approval statistics
     * - Team structure (all managers with their teams)
     */
    @GetMapping("/hr")
    public ResponseEntity<HRDashboardResponse> getHRDashboard() {

        log.info("🏢 [API] GET HR dashboard");

        try {
            HRDashboardResponse response = dashboardService.getHRDashboard();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error getting HR dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ADMIN DASHBOARD ENDPOINTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET ADMIN DASHBOARD
     * GET /api/dashboard/v2/admin/{adminId}
     *
     * Returns:
     * - Admin's own stats
     * - System compliance metrics
     * - Leave statistics (by type, usage, etc.)
     * - Rejected leaves audit trail
     * - Compliance issues & recommendations
     * - New employees onboarding status
     */
    @GetMapping("/admin/{adminId}")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard(
            @PathVariable Long adminId) {

        log.info("⚙️ [API] GET admin dashboard: {}", adminId);

        try {
            AdminDashboardResponse response = dashboardService.getAdminDashboard(adminId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error getting admin dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ADMIN AUTOMATION ENDPOINTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * TRIGGER YEAR-END CARRY FORWARD (Admin only)
     * POST /api/dashboard/v2/admin/run-year-end?year=2025
     *
     * Manual trigger for year-end carry forward processing
     * Processes all employees' year-end balance calculation
     */
    @PostMapping("/admin/run-year-end")
    public ResponseEntity<String> triggerYearEndProcessing(
            @RequestParam Integer year) {

        log.info("🔧 [API] Admin triggered year-end processing for year: {}", year);

        try {
            carryForwardScheduler.triggerYearEndProcessing(year);
            return ResponseEntity.ok("Year-end carry forward processing completed for year: " + year);
        } catch (Exception e) {
            log.error("❌ Year-end processing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
