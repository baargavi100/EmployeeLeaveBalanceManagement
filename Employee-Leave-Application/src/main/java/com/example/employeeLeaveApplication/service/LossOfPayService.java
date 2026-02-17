package com.example.employeeLeaveApplication.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.employeeLeaveApplication.constants.PolicyConstants;
import com.example.employeeLeaveApplication.entity.LossOfPayRecord;
import com.example.employeeLeaveApplication.repository.LossOfPayRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LossOfPayService {

    private static final Logger log = LoggerFactory.getLogger(LossOfPayService.class);
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

        // Calculate loss percentage using centralized constant
        double lossPercentage = excessDays * PolicyConstants.LOSS_OF_PAY_PERCENT_PER_DAY;

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
     * Get all LOP records for an employee (all years)
     */
    public java.util.List<LossOfPayRecord> getAllForEmployee(Long empId) {
        return lopRepo.findByEmployeeIdOrderByYearDescMonthDesc(empId);
    }

    /**
     * Get LOP records for an employee in a specific year
     */
    public java.util.List<LossOfPayRecord> getForEmployeeAndYear(Long empId, Integer year) {
        return lopRepo.findByEmployeeIdAndYear(empId, year);
    }
    // ═══════════════════════════════════════════════════════════════════
// ADD THIS METHOD to LossOfPayService.java
// ═══════════════════════════════════════════════════════════════════

    /**
     * Restore loss of pay (when leave is cancelled)
     * Deletes the LOP record for that month
     */
    @Transactional
    public void restoreLossOfPay(Long empId, Integer year, Integer month) {

        log.info("[LOP-RESTORE] Restoring LOP: employee={}, year={}, month={}",
                empId, year, month);

        Optional<LossOfPayRecord>lopOpt = lopRepo.findByEmployeeIdAndYearAndMonth(empId, year, month);

        if (lopOpt.isPresent()) {
            lopRepo.delete(lopOpt.get());
            log.info("[LOP-RESTORE] Deleted LOP record for month {}", month);
        } else {
            log.warn("[LOP-RESTORE] No LOP record found to restore");
        }
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