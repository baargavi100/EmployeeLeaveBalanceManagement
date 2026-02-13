// ═══════════════════════════════════════════════════════════════════
// FILE: CompOffEarnRequest.java
// Location: src/main/java/com/example/notificationservice/dto/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.dto;

import jakarta.validation.constraints.*;

/**
 * Request DTO for earning comp-off days
 */
public class CompOffEarnRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Year is required")
    @Min(value = 2020, message = "Year must be 2020 or later")
    private Integer year;

    @NotNull(message = "Days to earn is required")
    @Positive(message = "Days must be positive")
    @DecimalMax(value = "1.0", message = "Maximum 1 comp-off day can be earned at a time")
    private Double daysToEarn;

    @NotBlank(message = "Reason is required")
    private String reason;

    // ═══════════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════════

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getDaysToEarn() {
        return daysToEarn;
    }

    public void setDaysToEarn(Double daysToEarn) {
        this.daysToEarn = daysToEarn;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}