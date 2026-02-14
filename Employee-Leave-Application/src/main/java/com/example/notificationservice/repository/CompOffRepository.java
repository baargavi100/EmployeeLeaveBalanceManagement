package com.example.notificationservice.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.notificationservice.entity.CompOff;
import com.example.notificationservice.enums.CompOffStatus;

public interface CompOffRepository extends JpaRepository<CompOff, Long> {

    boolean existsByEmployeeIdAndWorkedDate(Long employeeId, LocalDate workedDate);

    @Query("SELECT SUM(c.days) FROM CompOff c WHERE c.employeeId = :employeeId AND c.status = :status")
    BigDecimal sumDaysByEmployeeAndStatus(@Param("employeeId") Long employeeId, @Param("status") CompOffStatus status);

    List<CompOff> findByEmployeeIdAndStatusOrderByWorkedDateAsc(Long employeeId, CompOffStatus status);

    List<CompOff> findByStatus(CompOffStatus status);

    /**
     * Find the exact Comp-Off records linked to a specific leave application for reversal
     */
    List<CompOff> findByUsedLeaveApplicationId(Long applicationId);

    // ═══════════════════════════════════════════════════════════════
    // COMP-OFF EARNING & DASHBOARD QUERIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get earned CompOff (status = EARNED)
     */
    @Query("SELECT SUM(c.days) FROM CompOff c " +
            "WHERE c.employeeId = :employeeId AND c.status = 'EARNED'")
    BigDecimal getTotalEarned(@Param("employeeId") Long employeeId);

    /**
     * Get used CompOff (status = USED)
     */
    @Query("SELECT SUM(c.days) FROM CompOff c " +
            "WHERE c.employeeId = :employeeId AND c.status = 'USED'")
    BigDecimal getTotalUsed(@Param("employeeId") Long employeeId);

    /**
     * Get pending CompOff approvals (status = PENDING)
     */
    @Query("SELECT COUNT(c) FROM CompOff c " +
            "WHERE c.employeeId = :employeeId AND c.status = 'PENDING'")
    Integer countPendingApprovals(@Param("employeeId") Long employeeId);

    /**
     * Find earned CompOff in a year (for year-end processing)
     */
    @Query("SELECT c FROM CompOff c " +
            "WHERE c.employeeId = :employeeId AND c.status = 'EARNED' " +
            "AND YEAR(c.workedDate) = :year " +
            "ORDER BY c.workedDate DESC")
    List<CompOff> findEarnedInYear(@Param("employeeId") Long employeeId, @Param("year") Integer year);

    /**
     * Find all CompOff records for an employee (all statuses) ordered by date
     */
    @Query("SELECT c FROM CompOff c " +
            "WHERE c.employeeId = :employeeId " +
            "ORDER BY c.workedDate DESC")
    List<CompOff> findAllByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * Find pending CompOff for employee (needs approval)
     */
    @Query("SELECT c FROM CompOff c " +
            "WHERE c.employeeId = :employeeId AND c.status = 'PENDING' " +
            "ORDER BY c.workedDate DESC")
    List<CompOff> findPendingByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * Get detailed CompOff history with status breakdown
     */
    @Query("SELECT c FROM CompOff c " +
            "WHERE c.employeeId = :employeeId " +
            "ORDER BY c.workedDate DESC")
    List<CompOff> getCompOffHistory(@Param("employeeId") Long employeeId);
}
