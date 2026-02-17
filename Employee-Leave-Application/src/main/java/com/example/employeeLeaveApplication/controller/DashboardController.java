// ═══════════════════════════════════════════════════════════════════
// FILE: DashboardController.java (REFACTORED - Reusable for all roles)
// Location: src/main/java/com/example/notificationservice/controller/
// ═══════════════════════════════════════════════════════════════════

package com.example.employeeLeaveApplication.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.employeeLeaveApplication.dto.AdminDashboardResponse;
import com.example.employeeLeaveApplication.dto.EmployeeDashboardResponse;
import com.example.employeeLeaveApplication.dto.TeamMemberBalance;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    // ═══════════════════════════════════════════════════════════════
    // EMPLOYEE DASHBOARD (Also for Manager/Admin viewing their own)
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET EMPLOYEE DASHBOARD
     * GET /api/dashboard/employee/{employeeId}
     *
     * Usage:
     * - Employee viewing their own dashboard
     * - Manager viewing their own dashboard
     * - Admin viewing their own dashboard
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<EmployeeDashboardResponse> getEmployeeDashboard(
            @PathVariable Long employeeId) {

        log.info("📊 [API] GET employee dashboard: {}", employeeId);

        try {
            EmployeeDashboardResponse response = dashboardService.getEmployeeDashboard(employeeId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [API] Error getting employee dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET LEAVE COUNTS BY STATUS
     * GET /api/dashboard/leave-counts/{employeeId}?year=2025
     */
    @GetMapping("/leave-counts/{employeeId}")
    public ResponseEntity<Map<LeaveStatus, Long>> getLeaveCountsByStatus(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {

        log.info("📊 [API] GET leave counts: employee={}, year={}", employeeId, year);

        try {
            Map<LeaveStatus, Long> counts = dashboardService.getLeaveCountsByStatus(employeeId, year);
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            log.error("❌ [API] Error getting leave counts: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MANAGER DASHBOARD - Team View
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET TEAM BALANCES (Manager View)
     * GET /api/dashboard/manager/team-balances/{managerId}?year=2025
     */
    @GetMapping("/manager/team-balances/{managerId}")
    public ResponseEntity<List<TeamMemberBalance>> getTeamBalances(
            @PathVariable Long managerId,
            @RequestParam Integer year) {

        log.info("👥 [API] GET team balances: manager={}, year={}", managerId, year);

        try {
            List<TeamMemberBalance> balances = dashboardService.getTeamBalances(managerId, year);
            return ResponseEntity.ok(balances);
        } catch (Exception e) {
            log.error("❌ [API] Error getting team balances: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET PENDING TEAM REQUESTS COUNT
     * GET /api/dashboard/manager/pending-count/{managerId}
     */
    @GetMapping("/manager/pending-count/{managerId}")
    public ResponseEntity<Integer> getPendingCount(@PathVariable Long managerId) {

        log.info("📋 [API] GET pending count: manager={}", managerId);

        try {
            Integer count = dashboardService.getPendingTeamRequestsCount(managerId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("❌ [API] Error getting pending count: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET PENDING TEAM REQUESTS (Detailed)
     * GET /api/dashboard/manager/pending-requests/{managerId}
     */
    @GetMapping("/manager/pending-requests/{managerId}")
    public ResponseEntity<List<LeaveApplication>> getPendingRequests(
            @PathVariable Long managerId) {

        log.info("📋 [API] GET pending requests: manager={}", managerId);

        try {
            List<LeaveApplication> requests = dashboardService.getPendingTeamRequests(managerId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("❌ [API] Error getting pending requests: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HR DASHBOARD - Company-wide View
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET EMPLOYEES CURRENTLY ON LEAVE
     * GET /api/dashboard/hr/on-leave
     */
    @GetMapping("/hr/on-leave")
    public ResponseEntity<List<Employee>> getEmployeesOnLeave() {

        log.info("🏖️ [API] GET employees on leave");

        try {
            List<Employee> employees = dashboardService.getEmployeesCurrentlyOnLeave();
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            log.error("❌ [API] Error getting employees on leave: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET MANAGERS WITH UPCOMING LEAVE (Next 7 days)
     * GET /api/dashboard/hr/managers-upcoming-leave
     */
    @GetMapping("/hr/managers-upcoming-leave")
    public ResponseEntity<List<Employee>> getManagersWithUpcomingLeave() {

        log.info("👔 [API] GET managers with upcoming leave");

        try {
            List<Employee> managers = dashboardService.getManagersWithUpcomingLeave();
            return ResponseEntity.ok(managers);
        } catch (Exception e) {
            log.error("❌ [API] Error getting managers: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET ADMINS WITH UPCOMING LEAVE (Next 7 days)
     * GET /api/dashboard/hr/admins-upcoming-leave
     */
    @GetMapping("/hr/admins-upcoming-leave")
    public ResponseEntity<List<Employee>> getAdminsWithUpcomingLeave() {

        log.info("⚙️ [API] GET admins with upcoming leave");

        try {
            List<Employee> admins = dashboardService.getAdminsWithUpcomingLeave();
            return ResponseEntity.ok(admins);
        } catch (Exception e) {
            log.error("❌ [API] Error getting admins: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ADMIN DASHBOARD (Same as Employee Dashboard)
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET ADMIN DASHBOARD (Uses employee dashboard)
     * GET /api/dashboard/admin/{adminId}
     */
    @GetMapping("/admin/{adminId}")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard(
            @PathVariable Long adminId) {

        log.info("[API] GET admin dashboard: {}", adminId);

        try {
            AdminDashboardResponse response = dashboardService.getAdminDashboard(adminId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[API] Error getting admin dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}