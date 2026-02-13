// ═══════════════════════════════════════════════════════════════════
// FILE: EmployeeController.java
// Location: src/main/java/com/example/notificationservice/controller/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.controller;

import com.example.notificationservice.entity.Employee;
import com.example.notificationservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeRepository employeeRepo;

    // ═══════════════════════════════════════════════════════════════
    // GET ALL EMPLOYEES
    // ═══════════════════════════════════════════════════════════════

    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {

        log.info("[EMPLOYEE] Fetching all employees");

        List<Employee> employees = employeeRepo.findByActiveTrue();

        return ResponseEntity.ok(employees);
    }

    // ═══════════════════════════════════════════════════════════════
    // GET EMPLOYEE BY ID
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/{employeeId}")
    public ResponseEntity<?> getEmployeeById(@PathVariable Long employeeId) {

        log.info("[EMPLOYEE] Fetching employee: {}", employeeId);

        return employeeRepo.findById(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ═══════════════════════════════════════════════════════════════
    // CREATE EMPLOYEE
    // ═══════════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody Employee employee) {

        log.info("[EMPLOYEE] Creating employee: {}", employee.getEmail());

        try {
            // Check if email already exists
            if (employeeRepo.existsByEmail(employee.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Employee with this email already exists"
                ));
            }

            Employee saved = employeeRepo.save(employee);

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            log.error("[EMPLOYEE] Error creating employee", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to create employee",
                    "details", e.getMessage()
            ));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET TEAM MEMBERS FOR MANAGER
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/manager/{managerId}/team")
    public ResponseEntity<List<Employee>> getTeamMembers(@PathVariable Long managerId) {

        log.info("[EMPLOYEE] Fetching team members for manager: {}", managerId);

        List<Employee> team = employeeRepo.findActiveTeamMembers(managerId);

        return ResponseEntity.ok(team);
    }
}