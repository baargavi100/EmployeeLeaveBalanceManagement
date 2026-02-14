package com.example.notificationservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.enums.Role;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Find employee by email
     */
    Optional<Employee> findByEmail(String email);

    /**
     * Find all active employees
     */
    List<Employee> findByActiveTrue();

        /**
         * Find all active employees (alias)
         */
        @Query("SELECT e FROM Employee e WHERE e.active = true")
        List<Employee> findActiveEmployees();

    /**
     * Find all employees under a manager
     */
    List<Employee> findByManagerId(Long managerId);

        /**
         * Find employees by role
         */
        List<Employee> findByRole(Role role);

    /**
     * Check if employee exists
     */
    boolean existsByEmail(String email);

    /**
     * Get team members for a manager
     */
    @Query("SELECT e FROM Employee e WHERE e.managerId = :managerId AND e.active = true")
    List<Employee> findActiveTeamMembers(@Param("managerId") Long managerId);

    // ═══════════════════════════════════════════════════════════════
    // HR DASHBOARD QUERIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Find employees with pending onboarding (HR dashboard - new employees)
     */
    @Query("SELECT e FROM Employee e " +
            "WHERE (e.biometricStatus = 'PENDING' OR e.vpnStatus = 'PENDING') " +
            "AND e.onboardingCompletedAt IS NULL " +
            "ORDER BY e.joiningDate ASC")
    List<Employee> findOnboardingPending();

    /**
     * Find new employees (joined in last N days, not completed onboarding)
     */
    @Query("SELECT e FROM Employee e " +
            "WHERE e.joiningDate >= :fromDate " +
            "AND e.onboardingCompletedAt IS NULL " +
            "ORDER BY e.joiningDate DESC")
    List<Employee> findNewEmployeesSince(@Param("fromDate") LocalDate fromDate);

    /**
     * Find employees by manager for HR dashboard (team members with their manager)
     */
    @Query("SELECT e FROM Employee e WHERE e.managerId = :managerId AND e.active = true " +
            "ORDER BY e.name ASC")
    List<Employee> findTeamMembersByManager(@Param("managerId") Long managerId);

    /**
     * Get all managers (HR dashboard - to show managers with their team members)
     */
    @Query("SELECT DISTINCT e FROM Employee e WHERE e.role = 'MANAGER' AND e.active = true")
    List<Employee> findAllManagers();

    /**
     * Get employees with specific role
     */
    @Query("SELECT e FROM Employee e WHERE e.role = :role AND e.active = true ORDER BY e.name ASC")
    List<Employee> findActiveEmployeesByRole(@Param("role") Role role);

    /**
     * Check onboarding status - biometric pending
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.biometricStatus = 'PENDING' AND e.active = true")
    Integer countPendingBiometric();

    /**
     * Check onboarding status - VPN pending
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.vpnStatus = 'PENDING' AND e.active = true")
    Integer countPendingVPN();
}