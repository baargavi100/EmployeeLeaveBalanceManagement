// ═══════════════════════════════════════════════════════════════════
// FILE: DashboardService.java (REUSABLE for Employee/Manager/HR/Admin)
// Location: src/main/java/com/example/notificationservice/service/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.service;

import com.example.notificationservice.constants.PolicyConstants;
import com.example.notificationservice.dto.EmployeeDashboardResponse;
import com.example.notificationservice.dto.LeaveTypeBreakdown;
import com.example.notificationservice.dto.TeamMemberBalance;
import com.example.notificationservice.entity.*;
import com.example.notificationservice.enums.LeaveStatus;
import com.example.notificationservice.enums.LeaveType;
import com.example.notificationservice.enums.Role;
import com.example.notificationservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveApplicationRepository applicationRepository;
    private final CompOffBalanceRepository compOffRepository;
    private final CarryForwardBalanceRepository carryForwardRepository;
    private final LossOfPayRecordRepository lopRepository;

    // ═══════════════════════════════════════════════════════════════
    // EMPLOYEE DASHBOARD (Also used for Manager/Admin own dashboard)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get dashboard for any employee
     * This is reusable for:
     * - Employee viewing their own dashboard
     * - Manager viewing their own dashboard
     * - Admin viewing their own dashboard
     */
    public EmployeeDashboardResponse getEmployeeDashboard(Long employeeId) {

        log.info("📊 [DASHBOARD] Getting employee dashboard: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        EmployeeDashboardResponse response = new EmployeeDashboardResponse();
        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setCurrentYear(currentYear);
        response.setLastUpdated(LocalDateTime.now());

        // ═══════════════════════════════════════════════════════════
        // 1. YEARLY STATS (Total: 24 days)
        // ═══════════════════════════════════════════════════════════

        List<LeaveAllocation> allocations = allocationRepository
                .findByEmployeeIdAndYear(employeeId, currentYear);

        double yearlyAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        Double yearlyUsed = applicationRepository.getTotalUsedDays(
                employeeId, LeaveStatus.APPROVED, currentYear);
        if (yearlyUsed == null) yearlyUsed = 0.0;

        double yearlyBalance = yearlyAllocated - yearlyUsed;

        response.setYearlyAllocated(yearlyAllocated);
        response.setYearlyUsed(yearlyUsed);
        response.setYearlyBalance(yearlyBalance);

        log.info("   Yearly: Allocated={}, Used={}, Balance={}",
                yearlyAllocated, yearlyUsed, yearlyBalance);

        // ═══════════════════════════════════════════════════════════
        // 2. MONTHLY STATS
        // ═══════════════════════════════════════════════════════════

        response.setMonthlyAllocated(PolicyConstants.MONTHLY_LIMIT);

        Integer approvedThisMonth = applicationRepository
                .countApprovedInMonth(employeeId, currentYear, currentMonth);

        double monthlyUsed = approvedThisMonth != null ? approvedThisMonth.doubleValue() : 0.0;
        response.setMonthlyUsed(monthlyUsed);
        response.setMonthlyBalance(PolicyConstants.MONTHLY_LIMIT - monthlyUsed);

        log.info("   Monthly: Allocated={}, Used={}, Balance={}",
                PolicyConstants.MONTHLY_LIMIT, monthlyUsed,
                PolicyConstants.MONTHLY_LIMIT - monthlyUsed);

        // ═══════════════════════════════════════════════════════════
        // 3. CARRY FORWARD
        // ═══════════════════════════════════════════════════════════

        CarryForwardBalance cfBalance = carryForwardRepository
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .orElse(null);

        if (cfBalance != null) {
            response.setCarryForwardTotal(cfBalance.getTotalCarriedForward());
            response.setCarryForwardUsed(cfBalance.getTotalUsed());
            response.setCarryForwardRemaining(cfBalance.getRemaining());
        } else {
            response.setCarryForwardTotal(0.0);
            response.setCarryForwardUsed(0.0);
            response.setCarryForwardRemaining(0.0);
        }

        // ═══════════════════════════════════════════════════════════
        // 4. COMP-OFF BALANCE
        // ═══════════════════════════════════════════════════════════

        CompOffBalance compOff = compOffRepository
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .orElse(null);

        if (compOff != null) {
            response.setCompoffBalance(compOff.getBalance());
        } else {
            response.setCompoffBalance(0.0);
        }

        // ═══════════════════════════════════════════════════════════
        // 5. LOSS OF PAY
        // ═══════════════════════════════════════════════════════════

        Double totalLOP = lopRepository
                .getTotalLossPercentageByEmployeeIdAndYear(employeeId, currentYear);
        response.setLossOfPayPercentage(totalLOP != null ? totalLOP : 0.0);

        // ═══════════════════════════════════════════════════════════
        // 6. BREAKDOWN BY LEAVE TYPE
        // ═══════════════════════════════════════════════════════════

        List<LeaveApplication> approvedLeaves = applicationRepository
                .findByEmployeeIdAndStatusAndYear(
                        employeeId, LeaveStatus.APPROVED, currentYear);

        Map<LeaveType, List<LeaveApplication>> byType = approvedLeaves.stream()
                .collect(Collectors.groupingBy(LeaveApplication::getLeaveType));

        List<LeaveTypeBreakdown> breakdown = new ArrayList<>();

        for (LeaveAllocation alloc : allocations) {
            LeaveType type = LeaveType.valueOf(alloc.getLeaveCategory());
            double allocated = alloc.getAllocatedDays();

            double used = byType.getOrDefault(type, List.of())
                    .stream()
                    .mapToDouble(l -> l.getDays().doubleValue())
                    .sum();

            breakdown.add(new LeaveTypeBreakdown(
                    type, allocated, used, allocated - used, 0));
        }

        // Add COMP_OFF
        if (compOff != null) {
            breakdown.add(new LeaveTypeBreakdown(
                    LeaveType.COMP_OFF,
                    compOff.getEarned(),
                    compOff.getUsed(),
                    compOff.getBalance(),
                    0
            ));
        }

        response.setBreakdown(breakdown);

        log.info("✅ [DASHBOARD] Employee dashboard complete");

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // MANAGER DASHBOARD - Team View
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get team members' leave balance summary for manager
     */
    public List<TeamMemberBalance> getTeamBalances(Long managerId, Integer year) {

        log.info("👥 [DASHBOARD] Getting team balances for manager: {}", managerId);

        List<Employee> teamMembers = employeeRepository.findActiveTeamMembers(managerId);
        List<TeamMemberBalance> balances = new ArrayList<>();

        for (Employee member : teamMembers) {

            TeamMemberBalance balance = new TeamMemberBalance();
            balance.setEmployeeId(member.getId());
            balance.setEmployeeName(member.getName());

            // Get allocations
            Double allocated = allocationRepository
                    .getTotalAllocatedDays(member.getId(), year);
            balance.setTotalAllocated(allocated != null ? allocated : 0.0);

            // Get used
            Double used = applicationRepository
                    .getTotalUsedDays(member.getId(), LeaveStatus.APPROVED, year);
            balance.setTotalUsed(used != null ? used : 0.0);

            // Calculate remaining
            double remaining = balance.getTotalAllocated() - balance.getTotalUsed();
            balance.setTotalRemaining(remaining);

            // Get comp-off
            CompOffBalance compOff = compOffRepository
                    .findByEmployeeIdAndYear(member.getId(), year)
                    .orElse(null);
            balance.setCompOffBalance(compOff != null ? compOff.getBalance() : 0.0);

            // Get LOP
            Double lop = lopRepository
                    .getTotalLossPercentageByEmployeeIdAndYear(member.getId(), year);
            balance.setLopPercentage(lop != null ? lop : 0.0);

            balances.add(balance);
        }

        log.info("✅ [DASHBOARD] Retrieved {} team member balances", balances.size());

        return balances;
    }

    /**
     * Get pending team leave requests count
     */
    public Integer getPendingTeamRequestsCount(Long managerId) {
        Integer count = applicationRepository.countPendingTeamRequests(managerId);
        return count != null ? count : 0;
    }

    /**
     * Get pending team leave requests (detailed)
     */
    public List<LeaveApplication> getPendingTeamRequests(Long managerId) {
        return applicationRepository.findPendingTeamRequests(managerId);
    }

    // ═══════════════════════════════════════════════════════════════
    // HR DASHBOARD - Company-wide View
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get employees currently on leave (today)
     */
    public List<Employee> getEmployeesCurrentlyOnLeave() {

        log.info("🏖️ [DASHBOARD-HR] Getting employees currently on leave");

        LocalDate today = LocalDate.now();
        List<Long> employeeIds = applicationRepository
                .findEmployeesCurrentlyOnLeave(today);

        if (employeeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Employee> employees = employeeRepository.findAllById(employeeIds);

        log.info("✅ [DASHBOARD-HR] {} employees currently on leave", employees.size());

        return employees;
    }

    /**
     * Get all managers with upcoming leave (next 7 days)
     */
    public List<Employee> getManagersWithUpcomingLeave() {

        log.info("👔 [DASHBOARD-HR] Getting managers with upcoming leave");

        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        List<Employee> managers = employeeRepository.findByRole(Role.MANAGER);
        List<Employee> managersWithLeave = new ArrayList<>();

        for (Employee manager : managers) {
            List<LeaveApplication> upcomingLeaves = applicationRepository
                    .findByEmployeeIdAndStatus(manager.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !la.getStartDate().isBefore(today) &&
                            !la.getStartDate().isAfter(nextWeek))
                    .collect(Collectors.toList());

            if (!upcomingLeaves.isEmpty()) {
                managersWithLeave.add(manager);
            }
        }

        log.info("✅ [DASHBOARD-HR] {} managers with upcoming leave",
                managersWithLeave.size());

        return managersWithLeave;
    }

    /**
     * Get all admins with upcoming leave (next 7 days)
     */
    public List<Employee> getAdminsWithUpcomingLeave() {

        log.info("⚙️ [DASHBOARD-HR] Getting admins with upcoming leave");

        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        List<Employee> admins = employeeRepository.findByRole(Role.ADMIN);
        List<Employee> adminsWithLeave = new ArrayList<>();

        for (Employee admin : admins) {
            List<LeaveApplication> upcomingLeaves = applicationRepository
                    .findByEmployeeIdAndStatus(admin.getId(), LeaveStatus.APPROVED)
                    .stream()
                    .filter(la -> !la.getStartDate().isBefore(today) &&
                            !la.getStartDate().isAfter(nextWeek))
                    .collect(Collectors.toList());

            if (!upcomingLeaves.isEmpty()) {
                adminsWithLeave.add(admin);
            }
        }

        log.info("✅ [DASHBOARD-HR] {} admins with upcoming leave",
                adminsWithLeave.size());

        return adminsWithLeave;
    }

    // ═══════════════════════════════════════════════════════════════
    // ADMIN DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Admin uses same dashboard as employee for their own stats
     * Plus they can view onboarding pending list (if implemented)
     */
    public EmployeeDashboardResponse getAdminDashboard(Long adminId) {
        return getEmployeeDashboard(adminId);
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY: Get Leave Counts by Status
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get leave application counts by status for employee
     */
    public Map<LeaveStatus, Integer> getLeaveCountsByStatus(Long employeeId, Integer year) {

        Map<LeaveStatus, Integer> counts = new HashMap<>();

        counts.put(LeaveStatus.APPROVED,
                applicationRepository.countByStatus(employeeId, year, LeaveStatus.APPROVED));
        counts.put(LeaveStatus.PENDING,
                applicationRepository.countByStatus(employeeId, year, LeaveStatus.PENDING));
        counts.put(LeaveStatus.REJECTED,
                applicationRepository.countByStatus(employeeId, year, LeaveStatus.REJECTED));
        counts.put(LeaveStatus.CANCELLED,
                applicationRepository.countByStatus(employeeId, year, LeaveStatus.CANCELLED));

        return counts;
    }
}