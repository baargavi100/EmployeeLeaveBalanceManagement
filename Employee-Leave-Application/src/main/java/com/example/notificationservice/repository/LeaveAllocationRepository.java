package com.example.notificationservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.notificationservice.entity.LeaveAllocation;

@Repository
public interface LeaveAllocationRepository extends JpaRepository<LeaveAllocation, Long> {

    /**
     * Find all allocations for employee in a year
     */
    List<LeaveAllocation> findByEmployeeIdAndYear(Long employeeId, Integer year);
    
    /**
     * Check if allocation exists for employee, year, and category
     */
    boolean existsByEmployeeIdAndYearAndLeaveCategory(Long employeeId, Integer year, String leaveCategory);
}
