// ═══════════════════════════════════════════════════════════════════
// FILE: CompOffService.java
// Location: src/main/java/com/example/notificationservice/service/
// ═══════════════════════════════════════════════════════════════════

package com.example.employeeLeaveApplication.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.employeeLeaveApplication.entity.CompOff;
import com.example.employeeLeaveApplication.entity.CompOffBalance;
import com.example.employeeLeaveApplication.enums.CompOffStatus;
import com.example.employeeLeaveApplication.repository.CompOffBalanceRepository;
import com.example.employeeLeaveApplication.repository.CompOffRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompOffService {

    private static final Logger log = LoggerFactory.getLogger(CompOffService.class);
    
    private final CompOffBalanceRepository compOffRepository;
    private final CompOffRepository compOffRecordRepository;

    // ═══════════════════════════════════════════════════════════════
    // GET AVAILABLE COMP-OFF BALANCE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get available comp-off balance for employee
     */
    @Transactional(readOnly = true)
    public BigDecimal getAvailableCompOffDays(Long employeeId) {

        Double total = compOffRepository.getTotalAvailableBalance(employeeId);
        return BigDecimal.valueOf(total != null ? total : 0.0);
    }

    /**
     * Get comp-off balance for specific year
     */
    @Transactional(readOnly = true)
    public CompOffBalance getBalanceForYear(Long employeeId, Integer year) {
        return compOffRepository.findByEmployeeIdAndYear(employeeId, year)
                .orElse(null);
    }

    // ═══════════════════════════════════════════════════════════════
    // EARN COMP-OFF DAYS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Add earned comp-off days for employee
     */
    @Transactional
    public void earnCompOff(Long employeeId, Integer year, Double days) {

        log.info("💰 [COMPOFF] Earning comp-off: employee={}, year={}, days={}",
                employeeId, year, days);

        CompOffBalance balance = compOffRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElse(new CompOffBalance());

        balance.setEmployeeId(employeeId);
        balance.setYear(year);
        balance.setEarned(balance.getEarned() + days);
        balance.calculateBalance();

        compOffRepository.save(balance);

        log.info("✅ [COMPOFF] Earned {} days. New balance: {}",
                days, balance.getBalance());
    }

    // ═══════════════════════════════════════════════════════════════
    // COMP-OFF REQUEST & APPROVAL WORKFLOW (NEW)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Employee requests comp-off for working on holiday/weekend
     * Status: PENDING (awaiting team approval)
     */
    @Transactional
    public CompOff requestCompOff(Long employeeId, java.time.LocalDate workedDate,
                                   BigDecimal days, String description) {

        log.info("📝 [COMPOFF] Requesting comp-off: employee={}, worked={}, days={}",
                employeeId, workedDate, days);

        // Check if already exists for this date
        if (compOffRecordRepository.existsByEmployeeIdAndWorkedDate(employeeId, workedDate)) {
            throw new RuntimeException("CompOff request already exists for " + workedDate);
        }

        CompOff request = new CompOff();
        request.setEmployeeId(employeeId);
        request.setWorkedDate(workedDate);
        request.setDays(days);
        request.setDescription(description);
        request.setStatus(CompOffStatus.PENDING);

        compOffRecordRepository.save(request);

        log.info("✅ [COMPOFF] Request created with ID: {}, Status: PENDING", request.getId());

        return request;
    }

    /**
     * Approve pending comp-off request
     * Manager/Team lead approves → Status: EARNED → Balance added
     */
    @Transactional
    public CompOff approveCompOffRequest(Long compOffId) {

        log.info("✅ [COMPOFF] Approving comp-off request: {}", compOffId);

        CompOff compOff = compOffRecordRepository.findById(compOffId)
                .orElseThrow(() -> new RuntimeException("CompOff request not found: " + compOffId));

        if (compOff.getStatus() != CompOffStatus.PENDING) {
            throw new RuntimeException("Can only approve PENDING requests. Current status: " + compOff.getStatus());
        }

        // Change status to EARNED
        compOff.setStatus(CompOffStatus.EARNED);
        compOffRecordRepository.save(compOff);

        // Add to balance
        earnCompOff(compOff.getEmployeeId(), compOff.getWorkedDate().getYear(),
                compOff.getDays().doubleValue());

        log.info("✅ [COMPOFF] Request approved and balance updated");

        return compOff;
    }

    /**
     * Reject pending comp-off request
     */
    @Transactional
    public void rejectCompOffRequest(Long compOffId) {

        log.info("❌ [COMPOFF] Rejecting comp-off request: {}", compOffId);

        CompOff compOff = compOffRecordRepository.findById(compOffId)
                .orElseThrow(() -> new RuntimeException("CompOff request not found: " + compOffId));

        if (compOff.getStatus() != CompOffStatus.PENDING) {
            throw new RuntimeException("Can only reject PENDING requests. Current status: " + compOff.getStatus());
        }

        compOff.setStatus(CompOffStatus.REJECTED);
        compOffRecordRepository.save(compOff);

        log.info("✅ [COMPOFF] Request rejected");
    }

    /**
     * Get pending comp-off approvals for manager
     */
    @Transactional(readOnly = true)
    public List<CompOff> getPendingApprovals(Long managerId) {

        log.info("📋 [COMPOFF] Getting pending approvals for manager: {}", managerId);

        // TODO: Get all employees under this manager and fetch their pending CompOff requests
        // For now, return all pending
        return compOffRecordRepository.findByStatus(CompOffStatus.PENDING);
    }

    // ═══════════════════════════════════════════════════════════════
    // USE COMP-OFF DAYS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Use comp-off days (deduct from balance)
     * Returns actual days used (cannot go negative)
     */
    @Transactional
    public double useCompOff(Long employeeId, Integer year, Double daysRequested) {

        log.info("📤 [COMPOFF] Using comp-off: employee={}, year={}, requested={}",
                employeeId, year, daysRequested);

        CompOffBalance balance = compOffRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElseThrow(() -> new RuntimeException(
                        "No comp-off balance found for employee: " + employeeId));

        if (balance.getBalance() < daysRequested) {
            throw new RuntimeException(
                    "Insufficient comp-off balance. Available: " + balance.getBalance() +
                            ", Requested: " + daysRequested);
        }

        balance.setUsed(balance.getUsed() + daysRequested);
        balance.calculateBalance();

        compOffRepository.save(balance);

        log.info("✅ [COMPOFF] Used {} days. Remaining: {}",
                daysRequested, balance.getBalance());

        return daysRequested;
    }

    // ═══════════════════════════════════════════════════════════════
    // RESTORE COMP-OFF (When leave cancelled/rejected)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Restore comp-off days (when leave is cancelled/rejected)
     */
    @Transactional
    public void restoreCompOff(Long employeeId, Integer year, Double days) {

        log.info("🔄 [COMPOFF] Restoring comp-off: employee={}, year={}, days={}",
                employeeId, year, days);

        CompOffBalance balance = compOffRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElseThrow(() -> new RuntimeException(
                        "No comp-off balance found for employee: " + employeeId));

        balance.setUsed(Math.max(balance.getUsed() - days, 0.0));
        balance.calculateBalance();

        compOffRepository.save(balance);

        log.info("✅ [COMPOFF] Restored {} days. New balance: {}",
                days, balance.getBalance());
    }
}