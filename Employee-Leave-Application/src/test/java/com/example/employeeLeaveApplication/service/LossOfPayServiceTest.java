package com.example.employeeLeaveApplication.service;

import com.example.employeeLeaveApplication.constants.PolicyConstants;
import com.example.employeeLeaveApplication.entity.LossOfPayRecord;
import com.example.employeeLeaveApplication.repository.LossOfPayRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LossOfPayServiceTest {

    @Mock
    private LossOfPayRecordRepository lopRepo;

    @InjectMocks
    private LossOfPayService lossOfPayService;

    // ═══════════════════════════════════════════════════════════════
    // APPLY LOSS OF PAY
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Apply Loss of Pay")
    class ApplyLOPTests {

        @Test
        @DisplayName("Calculates LOP at 1% per excess day")
        void calculatesLOPCorrectly() {
            when(lopRepo.findByEmployeeIdAndYearAndMonth(1L, 2026, 3))
                    .thenReturn(Optional.empty());
            when(lopRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            lossOfPayService.applyLossOfPay(1L, 2026, 3, 3.0);

            ArgumentCaptor<LossOfPayRecord> captor = ArgumentCaptor.forClass(LossOfPayRecord.class);
            verify(lopRepo).save(captor.capture());

            LossOfPayRecord saved = captor.getValue();
            assertEquals(3.0, saved.getExcessDays());
            assertEquals(3.0 * PolicyConstants.LOSS_OF_PAY_PERCENT_PER_DAY, saved.getLossPercentage());
            assertEquals(2026, saved.getYear());
            assertEquals(3, saved.getMonth());
        }

        @Test
        @DisplayName("Updates existing LOP record for same month")
        void updatesExistingRecord() {
            LossOfPayRecord existing = new LossOfPayRecord();
            existing.setEmployeeId(1L);
            existing.setYear(2026);
            existing.setMonth(3);
            existing.setExcessDays(1.0);
            existing.setLossPercentage(1.0);

            when(lopRepo.findByEmployeeIdAndYearAndMonth(1L, 2026, 3))
                    .thenReturn(Optional.of(existing));
            when(lopRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            lossOfPayService.applyLossOfPay(1L, 2026, 3, 2.0);

            // Should update the existing record's excess days
            assertEquals(2.0, existing.getExcessDays());
            assertEquals(2.0, existing.getLossPercentage());
        }

        @Test
        @DisplayName("Rejects invalid month")
        void rejectsInvalidMonth() {
            assertThrows(IllegalArgumentException.class,
                    () -> lossOfPayService.applyLossOfPay(1L, 2026, 0, 1.0));

            assertThrows(IllegalArgumentException.class,
                    () -> lossOfPayService.applyLossOfPay(1L, 2026, 13, 1.0));

            assertThrows(IllegalArgumentException.class,
                    () -> lossOfPayService.applyLossOfPay(1L, 2026, null, 1.0));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET TOTAL LOSS OF PAY
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get Total LOP")
    class GetTotalLOPTests {

        @Test
        @DisplayName("Returns total LOP percentage for year")
        void returnsTotalForYear() {
            when(lopRepo.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(5.0);

            Double result = lossOfPayService.getTotalLossOfPayPercentage(1L, 2026);

            assertEquals(5.0, result);
        }

        @Test
        @DisplayName("Returns 0 when no LOP records")
        void returnsZeroWhenNull() {
            when(lopRepo.getTotalLossPercentageByEmployeeIdAndYear(1L, 2026))
                    .thenReturn(null);

            Double result = lossOfPayService.getTotalLossOfPayPercentage(1L, 2026);

            assertEquals(0.0, result);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // RESTORE LOSS OF PAY
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Restore LOP")
    class RestoreLOPTests {

        @Test
        @DisplayName("Deletes LOP record when restoring")
        void deletesRecordOnRestore() {
            LossOfPayRecord record = new LossOfPayRecord();
            record.setId(1L);
            record.setEmployeeId(1L);
            record.setYear(2026);
            record.setMonth(3);
            record.setExcessDays(2.0);
            record.setLossPercentage(2.0);

            when(lopRepo.findByEmployeeIdAndYearAndMonth(1L, 2026, 3))
                    .thenReturn(Optional.of(record));

            lossOfPayService.restoreLossOfPay(1L, 2026, 3);

            verify(lopRepo).delete(record);
        }

        @Test
        @DisplayName("Handles restore when no record exists (no error)")
        void handlesNoRecordGracefully() {
            when(lopRepo.findByEmployeeIdAndYearAndMonth(1L, 2026, 3))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(
                    () -> lossOfPayService.restoreLossOfPay(1L, 2026, 3));
        }
    }
}
