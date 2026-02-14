package com.example.notificationservice.component;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.notificationservice.service.CarryForwardService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Year-End Carry Forward Scheduler
 * Automatically processes carry forward on December 31st
 */
@Component
@RequiredArgsConstructor
public class CarryForwardScheduler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CarryForwardScheduler.class);

    private final CarryForwardService carryForwardService;

    /**
     * Run at 11:59 PM UTC on December 31st
     * Cron: 59 23 31 12 *
     * Triggers year-end carry forward processing for all employees
     */
    @Scheduled(cron = "0 0 1 1 *") // Runs at 00:00 on January 1st (for simplicity in testing)
    public void processYearEndCarryForward() {

        log.info("🎆🎆🎆 [SCHEDULER] Year-End Carry Forward processing started 🎆🎆🎆");

        try {
            int currentYear = LocalDate.now().getYear();
            int previousYear = currentYear - 1;

            log.info("📅 Processing carry forward for year: {} → {}", previousYear, currentYear);

            // Process carry forward for all employees
            carryForwardService.processYearEndCarryForward(previousYear);

            log.info("✅✅✅ [SCHEDULER] Year-End Carry Forward processing completed successfully ✅✅✅");

        } catch (Exception e) {
            log.error("❌❌❌ [SCHEDULER] Error during year-end carry forward processing: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for testing or admin use
     * Can be called via an endpoint
     */
    public void triggerYearEndProcessing(Integer forYear) {

        log.info("🔧 [MANUAL-TRIGGER] Admin triggered year-end processing for year: {}", forYear);

        try {
            carryForwardService.processYearEndCarryForward(forYear);
            log.info("✅ [MANUAL-TRIGGER] Year-end processing completed for year: {}", forYear);
        } catch (Exception e) {
            log.error("❌ [MANUAL-TRIGGER] Error: {}", e.getMessage(), e);
            throw new RuntimeException("Year-end processing failed: " + e.getMessage());
        }
    }
}
