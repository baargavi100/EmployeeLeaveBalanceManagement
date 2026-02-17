package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.entity.CompOff;
import com.example.employeeLeaveApplication.entity.CompOffBalance;
import com.example.employeeLeaveApplication.enums.CompOffStatus;
import com.example.employeeLeaveApplication.repository.CompOffBalanceRepository;
import com.example.employeeLeaveApplication.repository.CompOffRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CompOffServiceTest {

    @Mock
    private CompOffBalanceRepository compOffRepository;

    @Mock
    private CompOffRepository compOffRecordRepository;

    @InjectMocks
    private CompOffService compOffService;

    // ═══════════════════════════════════════════════════════════════
    // GET AVAILABLE COMP-OFF DAYS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get Available CompOff Days")
    class GetAvailableTests {

        @Test
        @DisplayName("Returns total available balance")
        void returnsTotalBalance() {
            when(compOffRepository.getTotalAvailableBalance(1L)).thenReturn(5.0);

            BigDecimal result = compOffService.getAvailableCompOffDays(1L);

            assertEquals(BigDecimal.valueOf(5.0), result);
        }

        @Test
        @DisplayName("Returns 0 when no balance")
        void returnsZeroWhenNull() {
            when(compOffRepository.getTotalAvailableBalance(1L)).thenReturn(null);

            BigDecimal result = compOffService.getAvailableCompOffDays(1L);

            assertEquals(BigDecimal.valueOf(0.0), result);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REQUEST COMP-OFF
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Request CompOff")
    class RequestCompOffTests {

        @Test
        @DisplayName("Creates PENDING comp-off request")
        void createsPendingRequest() {
            LocalDate workedDate = LocalDate.of(2026, 2, 14);
            when(compOffRecordRepository.existsByEmployeeIdAndWorkedDate(1L, workedDate))
                    .thenReturn(false);
            when(compOffRecordRepository.save(any())).thenAnswer(inv -> {
                CompOff co = inv.getArgument(0);
                co.setId(100L);
                return co;
            });

            CompOff result = compOffService.requestCompOff(
                    1L, workedDate, BigDecimal.valueOf(1.0), "Weekend work");

            assertEquals(CompOffStatus.PENDING, result.getStatus());
            assertEquals(1L, result.getEmployeeId());
            assertEquals(BigDecimal.valueOf(1.0), result.getDays());
        }

        @Test
        @DisplayName("Throws if request already exists for same date")
        void throwsIfDuplicateDate() {
            LocalDate workedDate = LocalDate.of(2026, 2, 14);
            when(compOffRecordRepository.existsByEmployeeIdAndWorkedDate(1L, workedDate))
                    .thenReturn(true);

            assertThrows(RuntimeException.class,
                    () -> compOffService.requestCompOff(1L, workedDate, BigDecimal.ONE, "Dup"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // APPROVE COMP-OFF REQUEST
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Approve CompOff Request")
    class ApproveCompOffTests {

        @Test
        @DisplayName("Approves PENDING request → EARNED + balance updated")
        void approvesPendingRequest() {
            CompOff compOff = new CompOff();
            compOff.setId(1L);
            compOff.setEmployeeId(1L);
            compOff.setStatus(CompOffStatus.PENDING);
            compOff.setDays(BigDecimal.valueOf(1.0));
            compOff.setWorkedDate(LocalDate.of(2026, 2, 14));

            CompOffBalance balance = new CompOffBalance();
            balance.setEmployeeId(1L);
            balance.setYear(2026);
            balance.setEarned(2.0);
            balance.setUsed(0.0);
            balance.setBalance(2.0);

            when(compOffRecordRepository.findById(1L)).thenReturn(Optional.of(compOff));
            when(compOffRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(compOffRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(balance));
            when(compOffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CompOff result = compOffService.approveCompOffRequest(1L);

            assertEquals(CompOffStatus.EARNED, result.getStatus());
            assertEquals(3.0, balance.getEarned()); // 2 + 1
        }

        @Test
        @DisplayName("Cannot approve non-PENDING request")
        void cannotApproveNonPending() {
            CompOff compOff = new CompOff();
            compOff.setId(1L);
            compOff.setStatus(CompOffStatus.EARNED);

            when(compOffRecordRepository.findById(1L)).thenReturn(Optional.of(compOff));

            assertThrows(RuntimeException.class,
                    () -> compOffService.approveCompOffRequest(1L));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REJECT COMP-OFF REQUEST
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Reject CompOff Request")
    class RejectCompOffTests {

        @Test
        @DisplayName("Rejects PENDING request → REJECTED")
        void rejectsPendingRequest() {
            CompOff compOff = new CompOff();
            compOff.setId(1L);
            compOff.setStatus(CompOffStatus.PENDING);

            when(compOffRecordRepository.findById(1L)).thenReturn(Optional.of(compOff));
            when(compOffRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            compOffService.rejectCompOffRequest(1L);

            assertEquals(CompOffStatus.REJECTED, compOff.getStatus());
        }

        @Test
        @DisplayName("Cannot reject non-PENDING request")
        void cannotRejectNonPending() {
            CompOff compOff = new CompOff();
            compOff.setId(1L);
            compOff.setStatus(CompOffStatus.EARNED);

            when(compOffRecordRepository.findById(1L)).thenReturn(Optional.of(compOff));

            assertThrows(RuntimeException.class,
                    () -> compOffService.rejectCompOffRequest(1L));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // USE COMP-OFF DAYS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Use CompOff Days")
    class UseCompOffTests {

        @Test
        @DisplayName("Deducts requested days from balance")
        void deductsFromBalance() {
            CompOffBalance balance = new CompOffBalance();
            balance.setEmployeeId(1L);
            balance.setYear(2026);
            balance.setEarned(5.0);
            balance.setUsed(1.0);
            balance.setBalance(4.0);

            when(compOffRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(balance));
            when(compOffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            double used = compOffService.useCompOff(1L, 2026, 2.0);

            assertEquals(2.0, used);
            assertEquals(3.0, balance.getUsed()); // 1 + 2
        }

        @Test
        @DisplayName("Throws if insufficient balance")
        void throwsIfInsufficient() {
            CompOffBalance balance = new CompOffBalance();
            balance.setEarned(2.0);
            balance.setUsed(1.0);
            balance.setBalance(1.0);

            when(compOffRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(balance));

            assertThrows(RuntimeException.class,
                    () -> compOffService.useCompOff(1L, 2026, 5.0));
        }

        @Test
        @DisplayName("Throws if no balance record exists")
        void throwsIfNoBalanceRecord() {
            when(compOffRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> compOffService.useCompOff(1L, 2026, 1.0));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // RESTORE COMP-OFF
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Restore CompOff")
    class RestoreCompOffTests {

        @Test
        @DisplayName("Restores used days on cancellation")
        void restoresUsedDays() {
            CompOffBalance balance = new CompOffBalance();
            balance.setEarned(5.0);
            balance.setUsed(3.0);
            balance.setBalance(2.0);

            when(compOffRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(balance));
            when(compOffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            compOffService.restoreCompOff(1L, 2026, 2.0);

            assertEquals(1.0, balance.getUsed()); // 3 - 2
        }

        @Test
        @DisplayName("Used doesn't go below 0")
        void usedDoesntGoBelowZero() {
            CompOffBalance balance = new CompOffBalance();
            balance.setEarned(5.0);
            balance.setUsed(1.0);
            balance.setBalance(4.0);

            when(compOffRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(balance));
            when(compOffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            compOffService.restoreCompOff(1L, 2026, 5.0);

            assertEquals(0.0, balance.getUsed()); // Math.max(1-5, 0) = 0
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // EARN COMP-OFF DAYS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Earn CompOff Days")
    class EarnCompOffTests {

        @Test
        @DisplayName("Adds earned days to existing balance")
        void addsToExistingBalance() {
            CompOffBalance balance = new CompOffBalance();
            balance.setEmployeeId(1L);
            balance.setYear(2026);
            balance.setEarned(3.0);
            balance.setUsed(0.0);
            balance.setBalance(3.0);

            when(compOffRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.of(balance));
            when(compOffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            compOffService.earnCompOff(1L, 2026, 2.0);

            assertEquals(5.0, balance.getEarned()); // 3 + 2
        }

        @Test
        @DisplayName("Creates new balance if none exists")
        void createsNewBalance() {
            when(compOffRepository.findByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(Optional.empty());
            when(compOffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            compOffService.earnCompOff(1L, 2026, 1.0);

            ArgumentCaptor<CompOffBalance> captor = ArgumentCaptor.forClass(CompOffBalance.class);
            verify(compOffRepository).save(captor.capture());

            assertEquals(1.0, captor.getValue().getEarned());
            assertEquals(1L, captor.getValue().getEmployeeId());
        }
    }
}
