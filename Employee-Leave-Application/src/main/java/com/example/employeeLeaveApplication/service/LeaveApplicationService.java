
package com.example.employeeLeaveApplication.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.HalfDayType;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveApplicationService {
    
    private static final Logger log = LoggerFactory.getLogger(LeaveApplicationService.class);

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final EmployeeRepository employeeRepository;

    // ═══════════════════════════════════════════════════════════════
    // CREATE LEAVE APPLICATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a new leave application
     */
    @Transactional
    public LeaveApplication createLeaveApplication(LeaveApplication leaveApplication) {

        log.info("📝 [CREATE] Creating leave application for employee: {}",
                leaveApplication.getEmployeeId());

        // Validate employee exists
        employeeRepository.findById(leaveApplication.getEmployeeId())
                .orElseThrow(() -> new RuntimeException(
                        "Employee not found: " + leaveApplication.getEmployeeId()));

        // Calculate days if not provided
        if (leaveApplication.getDays() == null) {
            BigDecimal calculatedDays = calculateLeaveDays(
                    leaveApplication.getStartDate(),
                    leaveApplication.getEndDate(),
                    leaveApplication.getHalfDayType()
            );
            leaveApplication.setDays(calculatedDays);
        }

        // Set initial status
        leaveApplication.setStatus(LeaveStatus.PENDING);

        // Auto-populate year from start date
        if (leaveApplication.getStartDate() != null) {
            leaveApplication.setYear(leaveApplication.getStartDate().getYear());
        }

        LeaveApplication saved = leaveApplicationRepository.save(leaveApplication);

        log.info("✅ [CREATE] Leave application created: ID={}, Days={}",
                saved.getId(), saved.getDays());

        return saved;
    }

    // ═══════════════════════════════════════════════════════════════
    // CALCULATE LEAVE DAYS (Excludes weekends)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calculate number of leave days between start and end date
     * Excludes weekends (Saturday & Sunday)
     * Handles half-day leaves
     */
    private BigDecimal calculateLeaveDays(LocalDate startDate, LocalDate endDate,
                                          HalfDayType halfDayType) {

        if (startDate == null || endDate == null) {
            throw new RuntimeException("Start date and end date are required");
        }

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("End date cannot be before start date");
        }

        // Handle half-day
        if (halfDayType != null) {
            return BigDecimal.valueOf(0.5);
        }

        // Count working days (exclude weekends)
        long totalDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();

            // Exclude Saturday and Sunday
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                totalDays++;
            }

            current = current.plusDays(1);
        }

        log.info("   Calculated days: {} (from {} to {})", totalDays, startDate, endDate);

        return BigDecimal.valueOf(totalDays);
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERY METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Find leave application by ID
     */
    public LeaveApplication findById(Long id) {
        return leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave application not found: " + id));
    }

    /**
     * Find all leave applications for employee
     */
    public List<LeaveApplication> findByEmployeeId(Long employeeId) {
        return leaveApplicationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    /**
     * Find leave applications by employee and status
     */
    public List<LeaveApplication> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status) {
        return leaveApplicationRepository.findByEmployeeIdAndStatus(employeeId, status);
    }

    /**
     * Find leave applications by employee and year
     */
    public List<LeaveApplication> findByEmployeeIdAndYear(Long employeeId, Integer year) {
        return leaveApplicationRepository.findByEmployeeIdAndYear(employeeId, year);
    }

    /**
     * Find pending leave applications for employee
     */
    public List<LeaveApplication> findPendingLeaves(Long employeeId) {
        return leaveApplicationRepository.findByEmployeeIdAndStatusOrderByCreatedAtDesc(
                employeeId, LeaveStatus.PENDING);
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE LEAVE APPLICATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Update leave application (before approval)
     */
    @Transactional
    public LeaveApplication updateLeaveApplication(Long leaveId, LeaveApplication updates) {

        log.info("📝 [UPDATE] Updating leave application: {}", leaveId);

        LeaveApplication existing = findById(leaveId);

        // Only allow updates if status is PENDING
        if (existing.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Cannot update leave in status: " + existing.getStatus());
        }

        // Update fields
        if (updates.getStartDate() != null) {
            existing.setStartDate(updates.getStartDate());
        }
        if (updates.getEndDate() != null) {
            existing.setEndDate(updates.getEndDate());
        }
        if (updates.getLeaveType() != null) {
            existing.setLeaveType(updates.getLeaveType());
        }
        if (updates.getHalfDayType() != null) {
            existing.setHalfDayType(updates.getHalfDayType());
        }
        if (updates.getReason() != null) {
            existing.setReason(updates.getReason());
        }

        // Recalculate days
        BigDecimal calculatedDays = calculateLeaveDays(
                existing.getStartDate(),
                existing.getEndDate(),
                existing.getHalfDayType()
        );
        existing.setDays(calculatedDays);

        // Update year
        existing.setYear(existing.getStartDate().getYear());

        LeaveApplication saved = leaveApplicationRepository.save(existing);

        log.info("✅ [UPDATE] Leave application updated: Days={}", saved.getDays());

        return saved;
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE LEAVE APPLICATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Delete leave application (only if PENDING)
     */
    @Transactional
    public void deleteLeaveApplication(Long leaveId) {

        log.info("🗑️ [DELETE] Deleting leave application: {}", leaveId);

        LeaveApplication leave = findById(leaveId);

        // Only allow deletion if status is PENDING
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException(
                    "Cannot delete leave in status: " + leave.getStatus() +
                            ". Use cancel instead.");
        }

        leaveApplicationRepository.delete(leave);

        log.info("✅ [DELETE] Leave application deleted");
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDATION: Check for overlapping leaves
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if employee has overlapping leave applications
     */
    public boolean hasOverlappingLeaves(Long employeeId, LocalDate startDate, LocalDate endDate) {

        log.info("🔍 [VALIDATE] Checking overlapping leaves for employee: {}", employeeId);

        List<LeaveApplication> existingLeaves = leaveApplicationRepository
                .findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED);

        for (LeaveApplication existing : existingLeaves) {
            // Check if dates overlap
            boolean overlaps = !endDate.isBefore(existing.getStartDate()) &&
                    !startDate.isAfter(existing.getEndDate());

            if (overlaps) {
                log.warn("   Overlapping leave found: {} to {}",
                        existing.getStartDate(), existing.getEndDate());
                return true;
            }
        }

        log.info("   No overlapping leaves found");
        return false;
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get total approved days for employee in year
     */
    public Double getTotalApprovedDays(Long employeeId, Integer year) {
        Double total = leaveApplicationRepository.getTotalUsedDays(
                employeeId, LeaveStatus.APPROVED, year);
        return total != null ? total : 0.0;
    }

    /**
     * Get total approved days for employee in specific month
     */
    public Double getTotalApprovedDaysInMonth(Long employeeId, Integer year, Integer month) {
        Double total = leaveApplicationRepository.getTotalApprovedDaysInMonth(
                employeeId, year, month);
        return total != null ? total : 0.0;
    }

    /**
     * Count leave applications by status
     */
    public Integer countByStatus(Long employeeId, Integer year, LeaveStatus status) {
        return leaveApplicationRepository.countByStatus(employeeId, year, status);
    }
}