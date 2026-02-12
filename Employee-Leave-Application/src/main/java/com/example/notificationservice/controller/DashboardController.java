// ═══════════════════════════════════════════════════════════════════
// FIXED DashboardController.java
// Copy to: src/main/java/com/example/notificationservice/controller/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.controller;

import com.example.notificationservice.dto.EmployeeDashboardResponse;
import com.example.notificationservice.dto.response.MonthlyStatsResponse;
import com.example.notificationservice.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Get employee dashboard - full overview
     * GET /employee/{employeeId}
     */
    @GetMapping("/{employeeId}")
    public EmployeeDashboardResponse getDashboard(@PathVariable Long employeeId) {
        return dashboardService.getDashboard(employeeId);
    }

    /**
     * Get monthly statistics for employee
     * GET /monthly-stats/{employeeId}?year=2025&month=1
     */
    @GetMapping("/monthly-stats/{employeeId}")
    public MonthlyStatsResponse getMonthlyStats(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return dashboardService.getMonthlyStats(employeeId, year, month);
    }
}