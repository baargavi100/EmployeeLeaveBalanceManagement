package com.example.notificationservice.service;

import com.example.notificationservice.entity.LossOfPayRecord;
import com.example.notificationservice.repository.LossOfPayRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LossOfPayService {
    // In LossOfPayService.java
    public void applyMonthlyLimitViolation(Long employeeId, int year, int month) {
        // Implement logic to handle LOP limit violations
    }

    private final LossOfPayRecordRepository lopRepo;

    /**
     * Apply loss of pay for excess days (after carry forward is used)
     * Rule: 1% per excess day
     */
    @Transactional
    public void applyLossOfPay(Long empId, Integer year, Integer month, Double excessDays) {

        validateMonth(month);

        log.info("[LOP] Applying loss of pay: employee={}, year={}, month={}, excessDays={}",
                empId, year, month, excessDays);

        // Calculate loss percentage (1% per day)
        double lossPercentage = excessDays * 1.0;

        // Get or create LOP record
        LossOfPayRecord lop = getOrCreate(empId, year, month);

        // Update the record
        lop.setExcessDays(excessDays);
        lop.setLossPercentage(lossPercentage);
        lop.setUpdatedAt(LocalDateTime.now());

        lopRepo.save(lop);

        log.info("[LOP] Applied {}% loss of pay for {} excess days", lossPercentage, excessDays);
    }

    /**
     * Get total accumulated loss of pay for the year
     */
    public Double getTotalLossOfPayPercentage(Long empId, Integer year) {
        Double total = lopRepo.getTotalLossPercentageByEmployeeIdAndYear(empId, year);
        return total != null ? total : 0.0;
    }

    /**
     * Find existing LOP record or create new one
     */
    private LossOfPayRecord getOrCreate(Long empId, Integer year, Integer month) {

        return lopRepo.findByEmployeeIdAndYearAndMonth(empId, year, month)
                .orElseGet(() -> {
                    LossOfPayRecord lop = new LossOfPayRecord();
                    lop.setEmployeeId(empId);
                    lop.setYear(year);
                    lop.setMonth(month);
                    lop.setExcessDays(0.0);
                    lop.setLossPercentage(0.0);
                    lop.setCreatedAt(LocalDateTime.now());
                    lop.setUpdatedAt(LocalDateTime.now());
                    return lop;
                });
    }

    /**
     * Defensive validation
     */
    private void validateMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
    }
}