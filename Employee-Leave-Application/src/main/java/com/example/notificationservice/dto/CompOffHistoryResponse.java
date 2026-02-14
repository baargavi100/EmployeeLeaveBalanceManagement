package com.example.notificationservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CompOff History Response
 * Shows employee's comp-off earning and usage history
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompOffHistoryResponse {

    private Long employeeId;
    private String employeeName;
    private Integer currentYear;

    // ═══════════════════════════════════════════════════════════════
    // COMP-OFF BALANCE SUMMARY
    // ═══════════════════════════════════════════════════════════════

    private BigDecimal totalEarned;
    private BigDecimal totalUsed;
    private BigDecimal currentBalance;
    private Integer pendingApprovalsCount;

    // ═══════════════════════════════════════════════════════════════
    // COMP-OFF HISTORY BY STATUS
    // ═══════════════════════════════════════════════════════════════

    private List<CompOffRecordDTO> earnedRecords;
    private List<CompOffRecordDTO> usedRecords;
    private List<CompOffRecordDTO> pendingRecords;
    private List<CompOffRecordDTO> rejectedRecords;

    private LocalDateTime lastUpdated;

    /**
     * Nested DTO for individual comp-off record
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompOffRecordDTO {
        private Long id;
        private BigDecimal days;             // How many days earned/used
        private String status;              // PENDING, EARNED, USED, REJECTED
        private LocalDate workedDate;       // When the work was done (for earning)
        private LocalDate plannedLeaveDate; // When the leave was planned (can be null)
        private Long leaveApplicationId;    // Linked leave (if used)
        private String description;         // Reason for earning
        private LocalDateTime createdAt;
        private LocalDateTime lastUpdatedAt;
    }

    /**
     * Brief summary for dashboard
     */
    public CompOffSummaryDTO toSummary() {
        CompOffSummaryDTO summary = new CompOffSummaryDTO();
        summary.setEmployeeId(employeeId);
        summary.setEmployeeName(employeeName);
        summary.setTotalEarned(totalEarned);
        summary.setTotalUsed(totalUsed);
        summary.setBalance(currentBalance);
        summary.setPendingCount(pendingApprovalsCount);
        return summary;
    }
}

/**
 * Compact CompOff Summary for dashboard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class CompOffSummaryDTO {
    private Long employeeId;
    private String employeeName;
    private BigDecimal totalEarned;
    private BigDecimal totalUsed;
    private BigDecimal balance;
    private Integer pendingCount;
}
