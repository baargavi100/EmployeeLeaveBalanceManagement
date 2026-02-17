package com.example.employeeLeaveApplication.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.employeeLeaveApplication.constants.PolicyConstants;
import com.example.employeeLeaveApplication.dto.LeaveBalanceResponse;
import com.example.employeeLeaveApplication.entity.CarryForwardBalance;
import com.example.employeeLeaveApplication.entity.CompOffBalance;
import com.example.employeeLeaveApplication.entity.Employee;
import com.example.employeeLeaveApplication.entity.LeaveAllocation;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.repository.CarryForwardBalanceRepository;
import com.example.employeeLeaveApplication.repository.CompOffBalanceRepository;
import com.example.employeeLeaveApplication.repository.EmployeeRepository;
import com.example.employeeLeaveApplication.repository.LeaveAllocationRepository;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import com.example.employeeLeaveApplication.repository.LossOfPayRecordRepository;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveAllocationRepository allocationRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private CarryForwardBalanceRepository carryForwardBalanceRepository;

    @Mock
    private CompOffBalanceRepository compOffBalanceRepository;

    @Mock
    private LossOfPayRecordRepository lossOfPayRecordRepository;

    @InjectMocks
    private LeaveBalanceService leaveBalanceService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setName("John Doe");
        employee.setEmail("john@example.com");
        employee.setRole(Role.EMPLOYEE);
    }

    private LeaveAllocation createAllocation(String category, double days) {
        LeaveAllocation alloc = new LeaveAllocation();
        alloc.setEmployeeId(1L);
        alloc.setLeaveCategory(category);
        alloc.setYear(2026);
        alloc.setAllocatedDays(days);
        return alloc;
    }

    // ═══════════════════════════════════════════════════════════════
    // GET BALANCE - COMPREHENSIVE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get Complete Leave Balance")
    class GetBalanceTests {

        @Test
        @DisplayName("Returns correct total allocation (24 days)")
        void returnsCorrectTotalAllocation() {
            List<LeaveAllocation> allocations = Arrays.asList(
                    createAllocation("VACATION", 8.0),
                    createAllocation("SICK", 6.0),
                    createAllocation("CASUAL", 6.0),
                    createAllocation("PERSONAL", 4.0)
            );

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026)).thenReturn(allocations);
            when(leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                    1L, LeaveStatus.APPROVED, 2026)).thenReturn(Collections.emptyList());
            when(carryForwardBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(leaveApplicationRepository.countApprovedInMonth(eq(1L), eq(2026), anyInt()))
                    .thenReturn(0);
            when(lossOfPayRecordRepository.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(null);

            LeaveBalanceResponse result = leaveBalanceService.getBalance(1L, 2026);

            assertEquals(PolicyConstants.TOTAL_YEARLY_ALLOCATION, result.getTotalAllocated());
            assertEquals(0.0, result.getTotalUsed());
            assertEquals(PolicyConstants.TOTAL_YEARLY_ALLOCATION, result.getTotalRemaining());
        }

        @Test
        @DisplayName("Breakdown includes all 5 types (4 regular + COMP_OFF)")
        void breakdownIncludesAllTypes() {
            List<LeaveAllocation> allocations = Arrays.asList(
                    createAllocation("VACATION", 8.0),
                    createAllocation("SICK", 6.0),
                    createAllocation("CASUAL", 6.0),
                    createAllocation("PERSONAL", 4.0)
            );

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026)).thenReturn(allocations);
            when(leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                    1L, LeaveStatus.APPROVED, 2026)).thenReturn(Collections.emptyList());
            when(carryForwardBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(leaveApplicationRepository.countApprovedInMonth(eq(1L), eq(2026), anyInt()))
                    .thenReturn(0);
            when(lossOfPayRecordRepository.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(null);

            LeaveBalanceResponse result = leaveBalanceService.getBalance(1L, 2026);

            assertEquals(5, result.getBreakdown().size()); // 4 regular + COMP_OFF

            // Verify COMP_OFF is included
            assertTrue(result.getBreakdown().stream()
                    .anyMatch(b -> b.getLeaveType() == LeaveType.COMP_OFF));
        }

        @Test
        @DisplayName("COMP_OFF not counted in regular usage")
        void compOffNotCountedInRegularUsage() {
            List<LeaveAllocation> allocations = Arrays.asList(
                    createAllocation("VACATION", 8.0)
            );

            LeaveApplication vacationLeave = new LeaveApplication();
            vacationLeave.setLeaveType(LeaveType.VACATION);
            vacationLeave.setDays(BigDecimal.valueOf(2.0));

            LeaveApplication compOffLeave = new LeaveApplication();
            compOffLeave.setLeaveType(LeaveType.COMP_OFF);
            compOffLeave.setDays(BigDecimal.valueOf(1.0));

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026)).thenReturn(allocations);
            when(leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                    1L, LeaveStatus.APPROVED, 2026))
                    .thenReturn(Arrays.asList(vacationLeave, compOffLeave));
            when(carryForwardBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(leaveApplicationRepository.countApprovedInMonth(eq(1L), eq(2026), anyInt()))
                    .thenReturn(2);
            when(lossOfPayRecordRepository.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(null);

            LeaveBalanceResponse result = leaveBalanceService.getBalance(1L, 2026);

            // totalUsed should only count VACATION (2.0), NOT COMP_OFF (1.0)
            assertEquals(2.0, result.getTotalUsed());
        }

        @Test
        @DisplayName("Shows carry forward balance from last year")
        void showsCarryForwardBalance() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Collections.emptyList());
            when(leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                    1L, LeaveStatus.APPROVED, 2026)).thenReturn(Collections.emptyList());

            CarryForwardBalance cf = new CarryForwardBalance();
            cf.setTotalCarriedForward(8.0);
            cf.setTotalUsed(3.0);
            cf.setRemaining(5.0);
            when(carryForwardBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(cf));
            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(leaveApplicationRepository.countApprovedInMonth(eq(1L), eq(2026), anyInt()))
                    .thenReturn(0);
            when(lossOfPayRecordRepository.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(null);

            LeaveBalanceResponse result = leaveBalanceService.getBalance(1L, 2026);

            assertEquals(5.0, result.getCarriedFromLastYear());
        }

        @Test
        @DisplayName("Shows comp-off balance (earned, used, available)")
        void showsCompOffBalance() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Collections.emptyList());
            when(leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                    1L, LeaveStatus.APPROVED, 2026)).thenReturn(Collections.emptyList());
            when(carryForwardBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());

            CompOffBalance compOff = new CompOffBalance();
            compOff.setEarned(5.0);
            compOff.setUsed(2.0);
            compOff.setBalance(3.0);
            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(compOff));
            when(leaveApplicationRepository.countApprovedInMonth(eq(1L), eq(2026), anyInt()))
                    .thenReturn(0);
            when(lossOfPayRecordRepository.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(null);

            LeaveBalanceResponse result = leaveBalanceService.getBalance(1L, 2026);

            assertEquals(5.0, result.getCompOffEarned());
            assertEquals(2.0, result.getCompOffUsed());
            assertEquals(3.0, result.getCompOffBalance());
        }

        @Test
        @DisplayName("Shows LOP percentage")
        void showsLOPPercentage() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Collections.emptyList());
            when(leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                    1L, LeaveStatus.APPROVED, 2026)).thenReturn(Collections.emptyList());
            when(carryForwardBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(leaveApplicationRepository.countApprovedInMonth(eq(1L), eq(2026), anyInt()))
                    .thenReturn(0);
            when(lossOfPayRecordRepository.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(3.0);

            LeaveBalanceResponse result = leaveBalanceService.getBalance(1L, 2026);

            assertEquals(3.0, result.getLopPercentage());
        }

        @Test
        @DisplayName("Calculates eligible carry forward: balance ≤ 10 → carry all")
        void eligibleCarryForward_withinThreshold() {
            List<LeaveAllocation> allocations = Arrays.asList(
                    createAllocation("VACATION", 8.0),
                    createAllocation("SICK", 6.0),
                    createAllocation("CASUAL", 6.0),
                    createAllocation("PERSONAL", 4.0)
            ); // total = 24

            LeaveApplication used = new LeaveApplication();
            used.setLeaveType(LeaveType.VACATION);
            used.setDays(BigDecimal.valueOf(16.0)); // balance = 8

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026)).thenReturn(allocations);
            when(leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                    1L, LeaveStatus.APPROVED, 2026)).thenReturn(List.of(used));
            when(carryForwardBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(leaveApplicationRepository.countApprovedInMonth(eq(1L), eq(2026), anyInt()))
                    .thenReturn(0);
            when(lossOfPayRecordRepository.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(null);

            LeaveBalanceResponse result = leaveBalanceService.getBalance(1L, 2026);

            assertEquals(8.0, result.getEligibleToCarry()); // 8 ≤ 10, carry all
        }

        @Test
        @DisplayName("Calculates eligible carry forward: balance > 10 → carry max 10")
        void eligibleCarryForward_exceedsThreshold() {
            List<LeaveAllocation> allocations = Arrays.asList(
                    createAllocation("VACATION", 8.0),
                    createAllocation("SICK", 6.0),
                    createAllocation("CASUAL", 6.0),
                    createAllocation("PERSONAL", 4.0)
            ); // total = 24

            // No leaves used → balance = 24 > 10
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026)).thenReturn(allocations);
            when(leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                    1L, LeaveStatus.APPROVED, 2026)).thenReturn(Collections.emptyList());
            when(carryForwardBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(leaveApplicationRepository.countApprovedInMonth(eq(1L), eq(2026), anyInt()))
                    .thenReturn(0);
            when(lossOfPayRecordRepository.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(null);

            LeaveBalanceResponse result = leaveBalanceService.getBalance(1L, 2026);

            assertEquals(PolicyConstants.MAX_CARRY_FORWARD, result.getEligibleToCarry()); // max 10
        }

        @Test
        @DisplayName("Monthly limit exceeded detection")
        void monthlyLimitExceededDetection() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Collections.emptyList());
            when(leaveApplicationRepository.findByEmployeeIdAndStatusAndYear(
                    1L, LeaveStatus.APPROVED, 2026)).thenReturn(Collections.emptyList());
            when(carryForwardBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(leaveApplicationRepository.countApprovedInMonth(eq(1L), eq(2026), anyInt()))
                    .thenReturn(3); // 3 > 2 (limit)
            when(lossOfPayRecordRepository.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(null);

            LeaveBalanceResponse result = leaveBalanceService.getBalance(1L, 2026);

            assertTrue(result.getExceededMonthlyLimit());
            assertEquals(3, result.getCurrentMonthApproved());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // INITIALIZE ALLOCATIONS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Initialize Allocations")
    class InitializeAllocationsTests {

        @Test
        @DisplayName("Creates 4 allocations: VACATION(8), SICK(6), CASUAL(6), PERSONAL(4)")
        void createsFourAllocations() {
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Collections.emptyList());
            when(allocationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            leaveBalanceService.initializeAllocations(1L, 2026);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<LeaveAllocation>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(allocationRepository).saveAll(captor.capture());

            List<LeaveAllocation> saved = captor.getValue();
            assertEquals(4, saved.size());

            // No COMP_OFF in allocations
            assertTrue(saved.stream()
                    .noneMatch(a -> a.getLeaveCategory().equals("COMP_OFF")));

            // Verify individual allocations
            assertEquals(PolicyConstants.VACATION_YEARLY_ALLOCATION,
                    saved.stream().filter(a -> a.getLeaveCategory().equals("VACATION"))
                            .findFirst().get().getAllocatedDays());
            assertEquals(PolicyConstants.SICK_YEARLY_ALLOCATION,
                    saved.stream().filter(a -> a.getLeaveCategory().equals("SICK"))
                            .findFirst().get().getAllocatedDays());
            assertEquals(PolicyConstants.CASUAL_YEARLY_ALLOCATION,
                    saved.stream().filter(a -> a.getLeaveCategory().equals("CASUAL"))
                            .findFirst().get().getAllocatedDays());
            assertEquals(PolicyConstants.PERSONAL_YEARLY_ALLOCATION,
                    saved.stream().filter(a -> a.getLeaveCategory().equals("PERSONAL"))
                            .findFirst().get().getAllocatedDays());
        }

        @Test
        @DisplayName("Skips if allocations already exist")
        void skipsIfAlreadyExists() {
            when(allocationRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(List.of(createAllocation("VACATION", 8.0)));

            leaveBalanceService.initializeAllocations(1L, 2026);

            verify(allocationRepository, never()).saveAll(anyList());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HAS SUFFICIENT BALANCE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Has Sufficient Balance")
    class HasSufficientBalanceTests {

        @Test
        @DisplayName("COMP_OFF: checks comp-off balance, not allocation")
        void compOff_checksCompOffBalance() {
            CompOffBalance compOff = new CompOffBalance();
            compOff.setBalance(3.0);

            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(compOff));

            assertTrue(leaveBalanceService.hasSufficientBalance(
                    1L, 2026, LeaveType.COMP_OFF, 2.0));
        }

        @Test
        @DisplayName("COMP_OFF: insufficient balance returns false")
        void compOff_insufficientBalance() {
            CompOffBalance compOff = new CompOffBalance();
            compOff.setBalance(1.0);

            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(compOff));

            assertFalse(leaveBalanceService.hasSufficientBalance(
                    1L, 2026, LeaveType.COMP_OFF, 2.0));
        }

        @Test
        @DisplayName("COMP_OFF: no balance record returns false")
        void compOff_noBalanceRecord() {
            when(compOffBalanceRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());

            assertFalse(leaveBalanceService.hasSufficientBalance(
                    1L, 2026, LeaveType.COMP_OFF, 1.0));
        }

        @Test
        @DisplayName("Regular type: checks allocation - used")
        void regularType_checksAllocationMinusUsed() {
            LeaveAllocation alloc = createAllocation("VACATION", 8.0);

            when(allocationRepository.findByEmployeeIdAndYearAndLeaveCategory(
                    1L, 2026, "VACATION")).thenReturn(Optional.of(alloc));
            when(leaveApplicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2026))
                    .thenReturn(3.0);

            // Available = 8 - 3 = 5 >= 4 → true
            assertTrue(leaveBalanceService.hasSufficientBalance(
                    1L, 2026, LeaveType.VACATION, 4.0));
        }

        @Test
        @DisplayName("Regular type: insufficient returns false")
        void regularType_insufficientBalance() {
            LeaveAllocation alloc = createAllocation("SICK", 6.0);

            when(allocationRepository.findByEmployeeIdAndYearAndLeaveCategory(
                    1L, 2026, "SICK")).thenReturn(Optional.of(alloc));
            when(leaveApplicationRepository.getTotalUsedDays(1L, LeaveStatus.APPROVED, 2026))
                    .thenReturn(5.0);

            // Available = 6 - 5 = 1 < 3 → false
            assertFalse(leaveBalanceService.hasSufficientBalance(
                    1L, 2026, LeaveType.SICK, 3.0));
        }

        @Test
        @DisplayName("No allocation found → returns false")
        void noAllocationFound() {
            when(allocationRepository.findByEmployeeIdAndYearAndLeaveCategory(
                    1L, 2026, "VACATION")).thenReturn(Optional.empty());

            assertFalse(leaveBalanceService.hasSufficientBalance(
                    1L, 2026, LeaveType.VACATION, 1.0));
        }
    }
}
