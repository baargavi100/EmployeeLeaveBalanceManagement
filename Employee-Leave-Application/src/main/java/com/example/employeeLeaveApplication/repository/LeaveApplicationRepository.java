// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveApplicationRepository.java (COMPLETE - NO ERRORS)
// Location: src/main/java/com/example/notificationservice/repository/
// ═══════════════════════════════════════════════════════════════════

package com.example.employeeLeaveApplication.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    // ═══════════════════════════════════════════════════════════════
    // BASIC FINDERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Find all applications by employee and year
     */
    List<LeaveApplication> findByEmployeeIdAndYear(Long employeeId, Integer year);

    /**
     * Find all applications by employee, status, and year
     */
    List<LeaveApplication> findByEmployeeIdAndStatusAndYear(
            Long employeeId, LeaveStatus status, Integer year);

    /**
     * Find all applications by employee and status
     */
    List<LeaveApplication> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    /**
     * Find applications by employee ordered by created date (newest first)
     */
    List<LeaveApplication> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    /**
     * Find all applications by status
     */
    List<LeaveApplication> findByStatus(LeaveStatus status);

    /**
     * Find pending applications for employee ordered by created date
     */
    List<LeaveApplication> findByEmployeeIdAndStatusOrderByCreatedAtDesc(
            Long employeeId, LeaveStatus status);

    // ═══════════════════════════════════════════════════════════════
    // AGGREGATE QUERIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get total used days for employee in year (APPROVED only)
     */
    @Query("SELECT COALESCE(SUM(la.days), 0.0) " +
            "FROM LeaveApplication la " +
            "WHERE la.employeeId = :employeeId " +
            "AND la.status = :status " +
            "AND la.year = :year")
    Double getTotalUsedDays(@Param("employeeId") Long employeeId,
                            @Param("status") LeaveStatus status,
                            @Param("year") Integer year);

    /**
     * Count approved applications in specific month
     * IMPORTANT: Counts number of APPLICATIONS, not days
     */
    @Query("SELECT COUNT(la) " +
            "FROM LeaveApplication la " +
            "WHERE la.employeeId = :employeeId " +
            "AND la.year = :year " +
            "AND FUNCTION('MONTH', la.startDate) = :month " +
            "AND la.status = 'APPROVED'")
    Integer countApprovedInMonth(@Param("employeeId") Long employeeId,
                                 @Param("year") Integer year,
                                 @Param("month") Integer month);

    /**
     * Get cumulative approved DAYS in specific month
     * This is what we use for monthly limit calculation
     */
    @Query("SELECT COALESCE(SUM(la.days), 0.0) " +
            "FROM LeaveApplication la " +
            "WHERE la.employeeId = :employeeId " +
            "AND la.year = :year " +
            "AND FUNCTION('MONTH', la.startDate) = :month " +
            "AND la.status = 'APPROVED'")
    Double getTotalApprovedDaysInMonth(@Param("employeeId") Long employeeId,
                                       @Param("year") Integer year,
                                       @Param("month") Integer month);

    /**
     * Count applications by status
     */
    @Query("SELECT COUNT(la) FROM LeaveApplication la " +
            "WHERE la.employeeId = :employeeId " +
            "AND la.year = :year " +
            "AND la.status = :status")
    Integer countByStatus(@Param("employeeId") Long employeeId,
                          @Param("year") Integer year,
                          @Param("status") LeaveStatus status);

    // ═══════════════════════════════════════════════════════════════
    // LEAVE TYPE SPECIFIC QUERIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Find approved applications by leave type in year
     */
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.employeeId = :employeeId " +
            "AND la.year = :year " +
            "AND la.leaveType = :leaveType " +
            "AND la.status = 'APPROVED'")
    List<LeaveApplication> findApprovedByLeaveType(@Param("employeeId") Long employeeId,
                                                   @Param("year") Integer year,
                                                   @Param("leaveType") LeaveType leaveType);

    // ═══════════════════════════════════════════════════════════════
    // HR DASHBOARD QUERIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Find employees currently on leave (today's date is between start and end)
     */
    @Query("SELECT DISTINCT la.employeeId FROM LeaveApplication la " +
            "WHERE la.status = 'APPROVED' " +
            "AND :currentDate BETWEEN la.startDate AND la.endDate")
    List<Long> findEmployeesCurrentlyOnLeave(@Param("currentDate") LocalDate currentDate);

    /**
     * Find all approved leaves in a date range (for HR dashboard - who's on leave)
     */
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.status = 'APPROVED' " +
            "AND (:startDate <= la.endDate AND :endDate >= la.startDate) " +
            "ORDER BY la.startDate ASC")
    List<LeaveApplication> findApprovedLeavesInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find approved leaves by manager's approver role (for HR dashboard - leaves approved by managers)
     */
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.status = 'APPROVED' " +
            "AND la.approvedRole = 'MANAGER' " +
            "AND la.year = :year " +
            "ORDER BY la.approvedAt DESC")
    List<LeaveApplication> findLeavesApprovedByManagers(@Param("year") Integer year);

    /**
     * Get distinct managers who have approved leaves in a year
     */
    @Query("SELECT DISTINCT la.approvedBy FROM LeaveApplication la " +
            "WHERE la.status = 'APPROVED' " +
            "AND la.approvedRole = 'MANAGER' " +
            "AND la.year = :year")
    List<Long> findManagersWhoApprovedLeaves(@Param("year") Integer year);

    /**
     * Get leaves approved by specific manager
     */
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.status = 'APPROVED' " +
            "AND la.approvedBy = :managerId " +
            "AND la.year = :year " +
            "ORDER BY la.approvedAt DESC")
    List<LeaveApplication> findLeavesApprovedByManager(@Param("managerId") Long managerId, @Param("year") Integer year);

    // ═══════════════════════════════════════════════════════════════
    // MANAGER DASHBOARD QUERIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Find pending team leave requests for manager
     * This joins with Employee table to get manager's team members
     */
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.employeeId IN " +
            "(SELECT e.id FROM Employee e WHERE e.managerId = :managerId) " +
            "AND la.status = 'PENDING' " +
            "ORDER BY la.createdAt ASC")
    List<LeaveApplication> findPendingTeamRequests(@Param("managerId") Long managerId);

    /**
     * Count pending requests for manager's team
     */
    @Query("SELECT COUNT(la) FROM LeaveApplication la " +
            "WHERE la.employeeId IN " +
            "(SELECT e.id FROM Employee e WHERE e.managerId = :managerId) " +
            "AND la.status = 'PENDING'")
    Integer countPendingTeamRequests(@Param("managerId") Long managerId);

    /**
     * ALIAS METHOD for compatibility
     * Same as findPendingTeamRequests but with different name
     */
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.employeeId IN " +
            "(SELECT e.id FROM Employee e WHERE e.managerId = :managerId) " +
            "AND la.status = 'PENDING' " +
            "ORDER BY la.createdAt ASC")
    List<LeaveApplication> getPendingLeavesForManager(@Param("managerId") Long managerId);
}