package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.constants.PolicyConstants;
import com.example.employeeLeaveApplication.dto.CarryForwardBalanceResponse;
import com.example.employeeLeaveApplication.dto.CarryForwardEligibilityResponse;
import com.example.employeeLeaveApplication.entity.CarryForwardBalance;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.repository.CarryForwardBalanceRepository;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveAllocationRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarryForwardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveAllocationRepository allocationRepository;

    @Mock
    private LeaveApplicationRepository applicationRepository;

    @Mock
    private CarryForwardBalanceRepository carryForwardRepository;

    @InjectMocks
    private CarryForwardService carryForwardService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setName("John Doe");
        employee.setEmail("john@example.com");
        employee.setRole(Role.EMPLOYEE);
        employee.setActive(true);
    }

    private LeaveAllocation createAllocation(String category, double days) {
        LeaveAllocation alloc = new LeaveAllocation();
        alloc.setEmployeeId(1L);
        alloc.setLeaveCategory(category);
        alloc.setYear(2025);
        alloc.setAllocatedDays(days);
        return alloc;
    }

    // ═══════════════════════════════════════════════════════════════
    // PROCESS EMPLOYEE CARRY FORWARD
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Process Employee Carry Forward")
    class ProcessEmployeeCFTests {

        @Test
        @DisplayName("Balance ≤ 10: carry forward entire balance")
        void balanceLessThanThreshold_carryAll() {
            List<LeaveAllocation> allocations = Arrays.asList(
                    createAllocation("VACATION", 8.0),
                    createAllocation("SICK", 6.0),
                    createAllocation("CASUAL", 6.0),
                    createAllocation("PERSONAL", 4.0)
            ); // total = 24
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2025))
                    .thenReturn(allocations);
            when(applicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2025))
                    .thenReturn(16.0); // balance = 24-16 = 8 (≤10)
            when(carryForwardRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(carryForwardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            carryForwardService.processEmployeeCarryForward(1L, 2025);

            ArgumentCaptor<CarryForwardBalance> captor = ArgumentCaptor.forClass(CarryForwardBalance.class);
            verify(carryForwardRepository).save(captor.capture());

            CarryForwardBalance saved = captor.getValue();
            assertEquals(8.0, saved.getTotalCarriedForward());
            assertEquals(2026, saved.getYear()); // stored for next year
            assertEquals(0.0, saved.getTotalUsed());
            assertEquals(8.0, saved.getRemaining());
        }

        @Test
        @DisplayName("Balance > 10: carry forward maximum 10 days")
        void balanceMoreThanThreshold_carryMax10() {
            List<LeaveAllocation> allocations = Arrays.asList(
                    createAllocation("VACATION", 8.0),
                    createAllocation("SICK", 6.0),
                    createAllocation("CASUAL", 6.0),
                    createAllocation("PERSONAL", 4.0)
            ); // total = 24
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2025))
                    .thenReturn(allocations);
            when(applicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2025))
                    .thenReturn(10.0); // balance = 24-10 = 14 (>10)
            when(carryForwardRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(carryForwardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            carryForwardService.processEmployeeCarryForward(1L, 2025);

            ArgumentCaptor<CarryForwardBalance> captor = ArgumentCaptor.forClass(CarryForwardBalance.class);
            verify(carryForwardRepository).save(captor.capture());

            assertEquals(PolicyConstants.MAX_CARRY_FORWARD, captor.getValue().getTotalCarriedForward());
        }

        @Test
        @DisplayName("Balance = 0: no carry forward")
        void balanceZero_noCarryForward() {
            List<LeaveAllocation> allocations = Arrays.asList(
                    createAllocation("VACATION", 8.0),
                    createAllocation("SICK", 6.0),
                    createAllocation("CASUAL", 6.0),
                    createAllocation("PERSONAL", 4.0)
            ); // total = 24
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2025))
                    .thenReturn(allocations);
            when(applicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2025))
                    .thenReturn(24.0); // balance = 0

            carryForwardService.processEmployeeCarryForward(1L, 2025);

            verify(carryForwardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Null used days treated as 0 → full allocation carried")
        void nullUsedDays_treatedAsZero() {
            List<LeaveAllocation> allocations = Arrays.asList(
                    createAllocation("VACATION", 8.0),
                    createAllocation("SICK", 6.0),
                    createAllocation("CASUAL", 6.0),
                    createAllocation("PERSONAL", 4.0)
            ); // total = 24 → balance = 24 (>10 so max 10)
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2025))
                    .thenReturn(allocations);
            when(applicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2025))
                    .thenReturn(null);
            when(carryForwardRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(carryForwardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            carryForwardService.processEmployeeCarryForward(1L, 2025);

            ArgumentCaptor<CarryForwardBalance> captor = ArgumentCaptor.forClass(CarryForwardBalance.class);
            verify(carryForwardRepository).save(captor.capture());
            assertEquals(PolicyConstants.MAX_CARRY_FORWARD, captor.getValue().getTotalCarriedForward());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PROCESS YEAR-END (ALL EMPLOYEES)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Process Year-End for All Employees")
    class ProcessYearEndTests {

        @Test
        @DisplayName("Processes all active employees")
        void processesAllActiveEmployees() {
            Employee emp2 = new Employee();
            emp2.setId(2L);
            emp2.setName("Jane");
            emp2.setActive(true);

            when(employeeRepository.findByActiveTrue()).thenReturn(Arrays.asList(employee, emp2));
            when(allocationRepository.findByEmployeeIdAndYear(anyLong(), eq(2025)))
                    .thenReturn(Collections.emptyList());
            when(applicationRepository.getTotalUsedDays(anyLong(), eq(LeaveStatus.APPROVED), eq(2025)))
                    .thenReturn(0.0);

            carryForwardService.processYearEndCarryForward(2025);

            // Should attempt to process each employee even if no allocations
            verify(allocationRepository).findByEmployeeIdAndYear(1L, 2025);
            verify(allocationRepository).findByEmployeeIdAndYear(2L, 2025);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // USE CARRY FORWARD
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Use Carry Forward")
    class UseCFTests {

        @Test
        @DisplayName("Use carry forward: deducts requested days")
        void useCarryForward_deductsRequested() {
            CarryForwardBalance balance = new CarryForwardBalance();
            balance.setEmployeeId(1L);
            balance.setYear(2026);
            balance.setTotalCarriedForward(5.0);
            balance.setTotalUsed(0.0);
            balance.setRemaining(5.0);

            when(carryForwardRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(balance));
            when(carryForwardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            double used = carryForwardService.useCarryForward(1L, 2026, 2.0);

            assertEquals(2.0, used);
            assertEquals(2.0, balance.getTotalUsed());
            assertEquals(3.0, balance.getRemaining());
        }

        @Test
        @DisplayName("Use carry forward: partial if insufficient")
        void useCarryForward_partialIfInsufficient() {
            CarryForwardBalance balance = new CarryForwardBalance();
            balance.setEmployeeId(1L);
            balance.setYear(2026);
            balance.setTotalCarriedForward(3.0);
            balance.setTotalUsed(1.0);
            balance.setRemaining(2.0);

            when(carryForwardRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(balance));
            when(carryForwardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            double used = carryForwardService.useCarryForward(1L, 2026, 5.0);

            assertEquals(2.0, used); // only 2 available
            assertEquals(0.0, balance.getRemaining());
        }

        @Test
        @DisplayName("Use carry forward: returns 0 if no balance")
        void useCarryForward_returnsZeroIfNoBalance() {
            when(carryForwardRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());

            double used = carryForwardService.useCarryForward(1L, 2026, 3.0);

            assertEquals(0.0, used);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // RESTORE CARRY FORWARD
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Restore Carry Forward")
    class RestoreCFTests {

        @Test
        @DisplayName("Restore: adds back used days")
        void restore_addsBackUsedDays() {
            CarryForwardBalance balance = new CarryForwardBalance();
            balance.setEmployeeId(1L);
            balance.setYear(2026);
            balance.setTotalCarriedForward(5.0);
            balance.setTotalUsed(3.0);
            balance.setRemaining(2.0);

            when(carryForwardRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(balance));
            when(carryForwardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            carryForwardService.restoreCarryForward(1L, 2026, 2.0);

            assertEquals(1.0, balance.getTotalUsed());
            assertEquals(4.0, balance.getRemaining()); // 5 - 1 = 4
        }

        @Test
        @DisplayName("Restore: used doesn't go below 0")
        void restore_usedDoesntGoBelowZero() {
            CarryForwardBalance balance = new CarryForwardBalance();
            balance.setTotalCarriedForward(5.0);
            balance.setTotalUsed(1.0);
            balance.setRemaining(4.0);

            when(carryForwardRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(balance));
            when(carryForwardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            carryForwardService.restoreCarryForward(1L, 2026, 5.0);

            assertEquals(0.0, balance.getTotalUsed());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CHECK ELIGIBILITY
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Check Eligibility")
    class CheckEligibilityTests {

        @Test
        @DisplayName("Balance 8 → eligible 8 (within threshold)")
        void eligibleWithinThreshold() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2025))
                    .thenReturn(Arrays.asList(createAllocation("VACATION", 8.0)));
            when(applicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2025))
                    .thenReturn(0.0);

            CarryForwardEligibilityResponse result = carryForwardService.checkEligibility(1L, 2025);

            assertTrue(result.getEligible());
            assertEquals(8.0, result.getEligibleAmount());
        }

        @Test
        @DisplayName("Balance 15 → eligible 10 (max cap)")
        void eligibleAboveThreshold() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2025))
                    .thenReturn(Arrays.asList(
                            createAllocation("VACATION", 8.0),
                            createAllocation("SICK", 6.0),
                            createAllocation("CASUAL", 6.0)
                    )); // total = 20
            when(applicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2025))
                    .thenReturn(5.0); // balance = 15

            CarryForwardEligibilityResponse result = carryForwardService.checkEligibility(1L, 2025);

            assertTrue(result.getEligible());
            assertEquals(PolicyConstants.MAX_CARRY_FORWARD, result.getEligibleAmount());
        }

        @Test
        @DisplayName("Balance 0 → not eligible")
        void notEligible() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2025))
                    .thenReturn(Arrays.asList(createAllocation("VACATION", 8.0)));
            when(applicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2025))
                    .thenReturn(8.0); // balance = 0

            CarryForwardEligibilityResponse result = carryForwardService.checkEligibility(1L, 2025);

            assertFalse(result.getEligible());
            assertEquals(0.0, result.getEligibleAmount());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET BALANCE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get Balance")
    class GetBalanceTests {

        @Test
        @DisplayName("Returns balance when carry forward exists")
        void returnsBalanceWhenExists() {
            CarryForwardBalance cf = new CarryForwardBalance();
            cf.setTotalCarriedForward(5.0);
            cf.setTotalUsed(2.0);
            cf.setRemaining(3.0);

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(carryForwardRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(cf));

            CarryForwardBalanceResponse result = carryForwardService.getBalance(1L, 2026);

            assertEquals(5.0, result.getTotalCarriedForward());
            assertEquals(2.0, result.getTotalUsed());
            assertEquals(3.0, result.getRemaining());
        }

        @Test
        @DisplayName("Returns zeros when no carry forward")
        void returnsZerosWhenNotExists() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(carryForwardRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());

            CarryForwardBalanceResponse result = carryForwardService.getBalance(1L, 2026);

            assertEquals(0.0, result.getTotalCarriedForward());
            assertEquals(0.0, result.getTotalUsed());
            assertEquals(0.0, result.getRemaining());
        }
    }
}
