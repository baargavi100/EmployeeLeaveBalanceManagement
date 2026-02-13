// ═══════════════════════════════════════════════════════════════════
// FILE: CompOffService.java
// Location: src/main/java/com/example/notificationservice/service/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.service;

import com.example.notificationservice.entity.CompOffBalance;
import com.example.notificationservice.repository.CompOffBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompOffService {

    private final CompOffBalanceRepository compOffRepository;

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