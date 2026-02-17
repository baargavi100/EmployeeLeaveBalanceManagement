package com.example.employeeLeaveApplication.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.HalfDayType;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;

@ExtendWith(MockitoExtension.class)
class LeaveApplicationServiceTest {

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private LeaveApplicationService leaveApplicationService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setName("John Doe");
        employee.setEmail("john@example.com");
        employee.setRole(Role.EMPLOYEE);
    }

    // ═══════════════════════════════════════════════════════════════
    // CREATE LEAVE APPLICATION TESTS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Create Leave Application")
    class CreateLeaveApplicationTests {

        @Test
        @DisplayName("Should create leave application with calculated weekday days")
        void shouldCreateLeaveWithCalculatedDays() {
            LeaveApplication leave = new LeaveApplication();
            leave.setEmployeeId(1L);
            leave.setLeaveType(LeaveType.VACATION);
            // Mon 2026-02-16 to Fri 2026-02-20 = 5 weekdays
            leave.setStartDate(LocalDate.of(2026, 2, 16));
            leave.setEndDate(LocalDate.of(2026, 2, 20));
            leave.setReason("Vacation trip");

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                    .thenAnswer(inv -> {
                        LeaveApplication saved = inv.getArgument(0);
                        saved.setId(100L);
                        return saved;
                    });

            LeaveApplication result = leaveApplicationService.createLeaveApplication(leave);

            assertEquals(LeaveStatus.PENDING, result.getStatus());
            assertEquals(new BigDecimal("5"), result.getDays());
            assertEquals(2026, result.getYear());
            verify(leaveApplicationRepository).save(any(LeaveApplication.class));
        }

        @Test
        @DisplayName("Should exclude weekends from day calculation")
        void shouldExcludeWeekendsFromDayCalculation() {
            LeaveApplication leave = new LeaveApplication();
            leave.setEmployeeId(1L);
            leave.setLeaveType(LeaveType.CASUAL);
            // Mon 2026-02-16 to Sun 2026-02-22 = 5 weekdays (Sat+Sun excluded)
            leave.setStartDate(LocalDate.of(2026, 2, 16));
            leave.setEndDate(LocalDate.of(2026, 2, 22));
            leave.setReason("Family event");

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LeaveApplication result = leaveApplicationService.createLeaveApplication(leave);

            // 7 calendar days but only 5 are weekdays
            assertEquals(new BigDecimal("5"), result.getDays());
        }

        @Test
        @DisplayName("Should set half-day as 0.5 days")
        void shouldSetHalfDayAs0Point5() {
            LeaveApplication leave = new LeaveApplication();
            leave.setEmployeeId(1L);
            leave.setLeaveType(LeaveType.SICK);
            leave.setStartDate(LocalDate.of(2026, 2, 16));
            leave.setEndDate(LocalDate.of(2026, 2, 16));
            leave.setHalfDayType(HalfDayType.FIRST_HALF);
            leave.setReason("Doctor visit");

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LeaveApplication result = leaveApplicationService.createLeaveApplication(leave);

            assertEquals(new BigDecimal("0.5"), result.getDays());
        }

        @Test
        @DisplayName("Should use provided days if already set")
        void shouldUseProvidedDaysIfAlreadySet() {
            LeaveApplication leave = new LeaveApplication();
            leave.setEmployeeId(1L);
            leave.setLeaveType(LeaveType.PERSONAL);
            leave.setStartDate(LocalDate.of(2026, 3, 2));
            leave.setEndDate(LocalDate.of(2026, 3, 3));
            leave.setDays(BigDecimal.valueOf(2));
            leave.setReason("Personal work");

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LeaveApplication result = leaveApplicationService.createLeaveApplication(leave);

            assertEquals(BigDecimal.valueOf(2), result.getDays());
        }

        @Test
        @DisplayName("Should throw exception for non-existent employee")
        void shouldThrowExceptionForNonExistentEmployee() {
            LeaveApplication leave = new LeaveApplication();
            leave.setEmployeeId(999L);

            when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> leaveApplicationService.createLeaveApplication(leave));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE LEAVE APPLICATION TESTS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Update Leave Application")
    class UpdateLeaveApplicationTests {

        @Test
        @DisplayName("Should update PENDING leave application")
        void shouldUpdatePendingLeave() {
            LeaveApplication existing = new LeaveApplication();
            existing.setId(1L);
            existing.setEmployeeId(1L);
            existing.setStatus(LeaveStatus.PENDING);
            existing.setStartDate(LocalDate.of(2026, 3, 2));
            existing.setEndDate(LocalDate.of(2026, 3, 3));
            existing.setLeaveType(LeaveType.VACATION);
            existing.setReason("Original");

            LeaveApplication updates = new LeaveApplication();
            updates.setReason("Updated reason");

            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LeaveApplication result = leaveApplicationService.updateLeaveApplication(1L, updates);

            assertEquals("Updated reason", result.getReason());
        }

        @Test
        @DisplayName("Should NOT update APPROVED leave application")
        void shouldNotUpdateApprovedLeave() {
            LeaveApplication existing = new LeaveApplication();
            existing.setId(1L);
            existing.setStatus(LeaveStatus.APPROVED);

            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(existing));

            assertThrows(RuntimeException.class,
                    () -> leaveApplicationService.updateLeaveApplication(1L, new LeaveApplication()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE LEAVE APPLICATION TESTS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Delete Leave Application")
    class DeleteLeaveApplicationTests {

        @Test
        @DisplayName("Should delete PENDING leave application")
        void shouldDeletePendingLeave() {
            LeaveApplication existing = new LeaveApplication();
            existing.setId(1L);
            existing.setStatus(LeaveStatus.PENDING);

            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(existing));

            leaveApplicationService.deleteLeaveApplication(1L);

            verify(leaveApplicationRepository).delete(existing);
        }

        @Test
        @DisplayName("Should NOT delete APPROVED leave application")
        void shouldNotDeleteApprovedLeave() {
            LeaveApplication existing = new LeaveApplication();
            existing.setId(1L);
            existing.setStatus(LeaveStatus.APPROVED);

            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(existing));

            assertThrows(RuntimeException.class,
                    () -> leaveApplicationService.deleteLeaveApplication(1L));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // OVERLAP CHECK TESTS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Overlapping Leave Check")
    class OverlapCheckTests {

        @Test
        @DisplayName("Should detect overlapping leaves")
        void shouldDetectOverlappingLeaves() {
            LeaveApplication existingLeave = new LeaveApplication();
            existingLeave.setStartDate(LocalDate.of(2026, 3, 10));
            existingLeave.setEndDate(LocalDate.of(2026, 3, 15));

            when(leaveApplicationRepository.findByEmployeeIdAndStatus(1L, LeaveStatus.APPROVED))
                    .thenReturn(List.of(existingLeave));

            // Overlapping: new leave starts on Mar 12 (within existing Mar 10-15)
            boolean overlaps = leaveApplicationService.hasOverlappingLeaves(
                    1L, LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 18));

            assertTrue(overlaps);
        }

        @Test
        @DisplayName("Should NOT detect non-overlapping leaves")
        void shouldNotDetectNonOverlappingLeaves() {
            LeaveApplication existingLeave = new LeaveApplication();
            existingLeave.setStartDate(LocalDate.of(2026, 3, 10));
            existingLeave.setEndDate(LocalDate.of(2026, 3, 15));

            when(leaveApplicationRepository.findByEmployeeIdAndStatus(1L, LeaveStatus.APPROVED))
                    .thenReturn(List.of(existingLeave));

            // Non-overlapping: new leave starts after existing ends
            boolean overlaps = leaveApplicationService.hasOverlappingLeaves(
                    1L, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20));

            assertFalse(overlaps);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS TESTS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should return total approved days")
        void shouldReturnTotalApprovedDays() {
            when(leaveApplicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2026))
                    .thenReturn(5.0);

            Double total = leaveApplicationService.getTotalApprovedDays(1L, 2026);

            assertEquals(5.0, total);
        }

        @Test
        @DisplayName("Should return 0 when no approved days")
        void shouldReturnZeroWhenNoApprovedDays() {
            when(leaveApplicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2026))
                    .thenReturn(null);

            Double total = leaveApplicationService.getTotalApprovedDays(1L, 2026);

            assertEquals(0.0, total);
        }

        @Test
        @DisplayName("Should return monthly approved days")
        void shouldReturnMonthlyApprovedDays() {
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 2))
                    .thenReturn(1.5);

            Double total = leaveApplicationService.getTotalApprovedDaysInMonth(1L, 2026, 2);

            assertEquals(1.5, total);
        }
    }
}
