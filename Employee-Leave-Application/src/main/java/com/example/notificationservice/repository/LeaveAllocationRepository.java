// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveAllocationRepository.java
// Location: src/main/java/com/example/notificationservice/repository/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.notificationservice.entity.LeaveAllocation;

@Repository
public interface LeaveAllocationRepository extends JpaRepository<LeaveAllocation, Long> {

    /**
     * Find all allocations for employee in a year
     */
    List<LeaveAllocation> findByEmployeeIdAndYear(Long employeeId, Integer year);

    /**
     * Find specific allocation by employee, year, and category
     */
    Optional<LeaveAllocation> findByEmployeeIdAndYearAndLeaveCategory(
            Long employeeId, Integer year, String leaveCategory);

    /**
     * Check if allocation exists for employee, year, and category
     */
    boolean existsByEmployeeIdAndYearAndLeaveCategory(
            Long employeeId, Integer year, String leaveCategory);

    /**
     * Get total allocated days for employee in year
     */
    @Query("SELECT COALESCE(SUM(la.allocatedDays), 0.0) " +
            "FROM LeaveAllocation la " +
            "WHERE la.employeeId = :employeeId AND la.year = :year")
    Double getTotalAllocatedDays(@Param("employeeId") Long employeeId,
                                 @Param("year") Integer year);

    /**
     * Delete all allocations for employee in year
     */
    void deleteByEmployeeIdAndYear(Long employeeId, Integer year);
}