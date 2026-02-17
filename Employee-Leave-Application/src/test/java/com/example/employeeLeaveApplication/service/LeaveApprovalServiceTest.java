package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.dto.CarryForwardBalanceResponse;
import com.example.employeeLeaveApplication.dto.LeaveApprovalSimulationResponse;
import com.example.employeeLeaveApplication.entity.LeaveApplication;
import com.example.employeeLeaveApplication.enums.LeaveStatus;
import com.example.employeeLeaveApplication.enums.LeaveType;
import com.example.employeeLeaveApplication.enums.Role;
import com.example.employeeLeaveApplication.repository.LeaveApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveApprovalServiceTest {

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private CarryForwardService carryForwardService;

    @Mock
    private CompOffService compOffService;

    @Mock
    private LossOfPayService lossOfPayService;

    @InjectMocks
    private LeaveApprovalService leaveApprovalService;

    private LeaveApplication createLeave(Long id, Long empId, LeaveType type,
                                          double days, int month) {
        LeaveApplication leave = new LeaveApplication();
        leave.setId(id);
        leave.setEmployeeId(empId);
        leave.setLeaveType(type);
        leave.setDays(BigDecimal.valueOf(days));
        leave.setStartDate(LocalDate.of(2026, month, 10));
        leave.setEndDate(LocalDate.of(2026, month, 10 + (int) days));
        leave.setYear(2026);
        leave.setStatus(LeaveStatus.PENDING);
        leave.setReason("Test");
        leave.setCarryForwardUsed(0.0);
        leave.setCompOffUsed(0.0);
        leave.setLossOfPayApplied(0.0);
        return leave;
    }

    // ═══════════════════════════════════════════════════════════════
    // SIMULATE APPROVAL - COMP_OFF
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Simulate Approval - COMP_OFF Leave")
    class SimulateCompOffTests {

        @Test
        @DisplayName("COMP_OFF: sufficient balance → can approve directly")
        void compOff_sufficientBalance_canApprove() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.COMP_OFF, 1.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(compOffService.getAvailableCompOffDays(1L)).thenReturn(BigDecimal.valueOf(3.0));

            LeaveApprovalSimulationResponse result = leaveApprovalService.simulateApproval(1L);

            assertTrue(result.getCanApproveDirectly());
            assertFalse(result.getRequiresUserDecision());
        }

        @Test
        @DisplayName("COMP_OFF: insufficient balance → cannot approve")
        void compOff_insufficientBalance_cannotApprove() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.COMP_OFF, 2.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(compOffService.getAvailableCompOffDays(1L)).thenReturn(BigDecimal.valueOf(1.0));

            LeaveApprovalSimulationResponse result = leaveApprovalService.simulateApproval(1L);

            assertFalse(result.getCanApproveDirectly());
            assertFalse(result.getRequiresUserDecision());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SIMULATE APPROVAL - REGULAR LEAVE WITHIN MONTHLY LIMIT
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Simulate Approval - Within Monthly Limit (≤ 2 days)")
    class SimulateWithinLimitTests {

        @Test
        @DisplayName("Within limit: 0 used + 1 requested = 1 ≤ 2 → approve directly")
        void withinLimit_noExistingUsage() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.VACATION, 1.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 3))
                    .thenReturn(0.0);

            LeaveApprovalSimulationResponse result = leaveApprovalService.simulateApproval(1L);

            assertTrue(result.getCanApproveDirectly());
            assertFalse(result.getRequiresUserDecision());
        }

        @Test
        @DisplayName("Within limit: 1.5 used + 0.5 requested = 2.0 ≤ 2 → approve directly")
        void withinLimit_exactlyAtLimit() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.SICK, 0.5, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 3))
                    .thenReturn(1.5);

            LeaveApprovalSimulationResponse result = leaveApprovalService.simulateApproval(1L);

            assertTrue(result.getCanApproveDirectly());
            assertFalse(result.getRequiresUserDecision());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SIMULATE APPROVAL - EXCEEDS MONTHLY LIMIT
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Simulate Approval - Exceeds Monthly Limit (> 2 days)")
    class SimulateExceedsLimitTests {

        @Test
        @DisplayName("Exceeds limit but carry forward sufficient → auto-approve with CF")
        void exceedsLimit_carryForwardCovers() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.VACATION, 2.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 3))
                    .thenReturn(1.0); // 1 + 2 = 3, excess = 1

            CarryForwardBalanceResponse cfResponse = new CarryForwardBalanceResponse();
            cfResponse.setRemaining(5.0); // Enough CF
            when(carryForwardService.getBalance(1L, 2026)).thenReturn(cfResponse);

            LeaveApprovalSimulationResponse result = leaveApprovalService.simulateApproval(1L);

            assertTrue(result.getCanApproveDirectly());
            assertTrue(result.getCanUseCarryForward());
            assertFalse(result.getRequiresUserDecision());
        }

        @Test
        @DisplayName("Exceeds limit, CF insufficient → requires user decision (CompOff or LOP)")
        void exceedsLimit_cfInsufficient_requiresDecision() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.CASUAL, 3.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 3))
                    .thenReturn(1.0); // 1 + 3 = 4, excess = 2

            CarryForwardBalanceResponse cfResponse = new CarryForwardBalanceResponse();
            cfResponse.setRemaining(0.0); // No CF
            when(carryForwardService.getBalance(1L, 2026)).thenReturn(cfResponse);
            when(compOffService.getAvailableCompOffDays(1L)).thenReturn(BigDecimal.valueOf(5.0));

            LeaveApprovalSimulationResponse result = leaveApprovalService.simulateApproval(1L);

            assertFalse(result.getCanApproveDirectly());
            assertTrue(result.getRequiresUserDecision());
            assertEquals(2.0, result.getExcessDays());
            assertTrue(result.getCanUseCompOff());
            assertEquals(2.0, result.getLopPercentage()); // 2 days * 1% = 2%
        }

        @Test
        @DisplayName("Exceeds limit, CF and CompOff both insufficient → LOP only option")
        void exceedsLimit_allInsufficientExceptLOP() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.PERSONAL, 3.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 3))
                    .thenReturn(2.0); // 2 + 3 = 5, excess = 3

            CarryForwardBalanceResponse cfResponse = new CarryForwardBalanceResponse();
            cfResponse.setRemaining(0.0);
            when(carryForwardService.getBalance(1L, 2026)).thenReturn(cfResponse);
            when(compOffService.getAvailableCompOffDays(1L)).thenReturn(BigDecimal.ZERO);

            LeaveApprovalSimulationResponse result = leaveApprovalService.simulateApproval(1L);

            assertFalse(result.getCanApproveDirectly());
            assertTrue(result.getRequiresUserDecision());
            assertFalse(result.getCanUseCompOff());
            assertEquals(3.0, result.getLopPercentage()); // 3 excess * 1%
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // APPROVE LEAVE TESTS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Approve Leave - Actual Approval")
    class ApproveLeaveTests {

        @Test
        @DisplayName("COMP_OFF leave: deducts from comp-off balance")
        void approveCompOffLeave_deductsFromBalance() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.COMP_OFF, 1.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(compOffService.getAvailableCompOffDays(1L)).thenReturn(BigDecimal.valueOf(3.0));
            when(compOffService.useCompOff(1L, 2026, 1.0)).thenReturn(1.0);
            when(leaveApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            leaveApprovalService.approveLeave(1L, 10L, Role.MANAGER, null, null);

            verify(compOffService).useCompOff(1L, 2026, 1.0);
            assertEquals(LeaveStatus.APPROVED, leave.getStatus());
            assertEquals(10L, leave.getApprovedBy());
            assertEquals(Role.MANAGER, leave.getApprovedRole());
            assertEquals(1.0, leave.getCompOffUsed());
        }

        @Test
        @DisplayName("Within monthly limit: approve directly, no deductions")
        void approveWithinLimit_noDeductions() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.VACATION, 1.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 3))
                    .thenReturn(0.0);
            when(leaveApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            leaveApprovalService.approveLeave(1L, 10L, Role.MANAGER, null, null);

            assertEquals(LeaveStatus.APPROVED, leave.getStatus());
            verifyNoInteractions(compOffService, lossOfPayService);
        }

        @Test
        @DisplayName("Exceeds limit + carry forward covers → auto-uses CF")
        void approveExceedsLimit_cfCovers_autoUsesCF() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.VACATION, 2.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 3))
                    .thenReturn(1.0); // 1 + 2 = 3, excess = 1

            CarryForwardBalanceResponse cfResponse = new CarryForwardBalanceResponse();
            cfResponse.setRemaining(5.0);
            when(carryForwardService.getBalance(1L, 2026)).thenReturn(cfResponse);
            when(carryForwardService.useCarryForward(1L, 2026, 1.0)).thenReturn(1.0);
            when(leaveApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            leaveApprovalService.approveLeave(1L, 10L, Role.MANAGER, null, null);

            verify(carryForwardService).useCarryForward(1L, 2026, 1.0);
            assertEquals(1.0, leave.getCarryForwardUsed());
            assertEquals(LeaveStatus.APPROVED, leave.getStatus());
        }

        @Test
        @DisplayName("Exceeds limit + user chooses CompOff → deducts comp-off")
        void approveExceedsLimit_userChoosesCompOff() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.CASUAL, 3.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 3))
                    .thenReturn(1.0); // excess = 2

            CarryForwardBalanceResponse cfResponse = new CarryForwardBalanceResponse();
            cfResponse.setRemaining(0.0);
            when(carryForwardService.getBalance(1L, 2026)).thenReturn(cfResponse);
            when(compOffService.getAvailableCompOffDays(1L)).thenReturn(BigDecimal.valueOf(5.0));
            when(compOffService.useCompOff(1L, 2026, 2.0)).thenReturn(2.0);
            when(leaveApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            leaveApprovalService.approveLeave(1L, 10L, Role.MANAGER, true, false);

            verify(compOffService).useCompOff(1L, 2026, 2.0);
            assertEquals(2.0, leave.getCompOffUsed());
            assertEquals(0.0, leave.getLossOfPayApplied());
        }

        @Test
        @DisplayName("Exceeds limit + user chooses LOP → records loss of pay")
        void approveExceedsLimit_userChoosesLOP() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.PERSONAL, 3.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 3))
                    .thenReturn(1.0); // excess = 2

            CarryForwardBalanceResponse cfResponse = new CarryForwardBalanceResponse();
            cfResponse.setRemaining(0.0);
            when(carryForwardService.getBalance(1L, 2026)).thenReturn(cfResponse);
            when(compOffService.getAvailableCompOffDays(1L)).thenReturn(BigDecimal.ZERO);
            when(leaveApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            leaveApprovalService.approveLeave(1L, 10L, Role.HR, false, true);

            verify(lossOfPayService).applyLossOfPay(1L, 2026, 3, 2.0);
            assertEquals(2.0, leave.getLossOfPayApplied()); // 2 excess * 1% = 2%
            assertEquals(0.0, leave.getCompOffUsed());
        }

        @Test
        @DisplayName("Exceeds limit + no choice made → throws exception")
        void approveExceedsLimit_noChoice_throws() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.VACATION, 3.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.getTotalApprovedDaysInMonth(1L, 2026, 3))
                    .thenReturn(1.0);

            CarryForwardBalanceResponse cfResponse = new CarryForwardBalanceResponse();
            cfResponse.setRemaining(0.0);
            when(carryForwardService.getBalance(1L, 2026)).thenReturn(cfResponse);
            when(compOffService.getAvailableCompOffDays(1L)).thenReturn(BigDecimal.ZERO);

            assertThrows(RuntimeException.class,
                    () -> leaveApprovalService.approveLeave(1L, 10L, Role.MANAGER, false, false));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REJECT LEAVE TESTS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Reject Leave")
    class RejectLeaveTests {

        @Test
        @DisplayName("Reject PENDING leave → sets REJECTED status")
        void rejectPendingLeave() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.VACATION, 1.0, 3);
            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            leaveApprovalService.rejectLeave(1L, 10L, Role.MANAGER);

            assertEquals(LeaveStatus.REJECTED, leave.getStatus());
            assertEquals(10L, leave.getApprovedBy());
        }

        @Test
        @DisplayName("Reject APPROVED leave → restores CompOff + CF + LOP")
        void rejectApprovedLeave_restoresAll() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.VACATION, 3.0, 3);
            leave.setStatus(LeaveStatus.APPROVED);
            leave.setCompOffUsed(1.0);
            leave.setCarryForwardUsed(1.0);
            leave.setLossOfPayApplied(1.0);

            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            leaveApprovalService.rejectLeave(1L, 10L, Role.ADMIN);

            verify(compOffService).restoreCompOff(1L, 2026, 1.0);
            verify(carryForwardService).restoreCarryForward(1L, 2026, 1.0);
            verify(lossOfPayService).restoreLossOfPay(1L, 2026, 3);
            assertEquals(LeaveStatus.REJECTED, leave.getStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCEL LEAVE TESTS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cancel Leave")
    class CancelLeaveTests {

        @Test
        @DisplayName("Cancel APPROVED leave → restores all deductions")
        void cancelApprovedLeave_restoresAll() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.CASUAL, 3.0, 3);
            leave.setStatus(LeaveStatus.APPROVED);
            leave.setCompOffUsed(1.0);
            leave.setCarryForwardUsed(0.5);
            leave.setLossOfPayApplied(1.5);

            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
            when(leaveApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            leaveApprovalService.cancelLeave(1L);

            verify(compOffService).restoreCompOff(1L, 2026, 1.0);
            verify(carryForwardService).restoreCarryForward(1L, 2026, 0.5);
            verify(lossOfPayService).restoreLossOfPay(1L, 2026, 3);
            assertEquals(LeaveStatus.CANCELLED, leave.getStatus());
            assertEquals(0.0, leave.getCompOffUsed());
            assertEquals(0.0, leave.getCarryForwardUsed());
            assertEquals(0.0, leave.getLossOfPayApplied());
        }

        @Test
        @DisplayName("Cancel non-APPROVED leave → throws exception")
        void cancelNonApprovedLeave_throws() {
            LeaveApplication leave = createLeave(1L, 1L, LeaveType.VACATION, 1.0, 3);
            leave.setStatus(LeaveStatus.PENDING);

            when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));

            assertThrows(RuntimeException.class,
                    () -> leaveApprovalService.cancelLeave(1L));
        }
    }
}
