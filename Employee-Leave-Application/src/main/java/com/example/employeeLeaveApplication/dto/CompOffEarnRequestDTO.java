package com.example.employeeLeaveApplication.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CompOff Earn Request
 * Used for managers to mark employees as having earned comp-off
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompOffEarnRequestDTO {

    private Long employeeId;              // Employee who earned comp-off
    private LocalDate workedDate;         // Date when work was done
    private BigDecimal days;              // 1.0 for full day, 0.5 for half day
    private LocalDate plannedLeaveDate;  // (Optional) When employee plans to use it
    private String description;           // Reason (e.g., "Worked on Sunday", "Holiday work")

    // ═══════════════════════════════════════════════════════════════
    // VALIDATION CONSTRAINTS
    // ═══════════════════════════════════════════════════════════════

    public boolean isValid() {
        return employeeId != null &&
                employeeId > 0 &&
                workedDate != null &&
                days != null &&
                (days.doubleValue() == 0.5 || days.doubleValue() == 1.0);
    }
}
