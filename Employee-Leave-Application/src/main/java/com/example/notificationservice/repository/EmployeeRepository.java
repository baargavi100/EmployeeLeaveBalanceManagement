// ═══════════════════════════════════════════════════════════════════
// FILE: EmployeeRepository.java
// Location: src/main/java/com/example/notificationservice/repository/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.repository;

import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     * Find all employees under a manager
     */
    List<Employee> findByManagerId(Long managerId);

    /**
     * Find all employees by role
     */
    List<Employee> findByRole(Role role);

    /**
     * Find active employees by role
     */
    List<Employee> findByRoleAndActiveTrue(Role role);

    /**
     * Check if employee exists
     */
    boolean existsByEmail(String email);

    /**
     * Get team members for a manager
     */
    @Query("SELECT e FROM Employee e WHERE e.managerId = :managerId AND e.active = true")
    List<Employee> findActiveTeamMembers(@Param("managerId") Long managerId);
}