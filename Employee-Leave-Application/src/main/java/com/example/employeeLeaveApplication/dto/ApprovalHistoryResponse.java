package com.example.employeeLeaveApplication.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Approval History Response
 * Shows leave approval audit trail with manager names
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalHistoryResponse {

    private Long employeeId;
    private String employeeName;
    private Integer currentYear;
    private Integer totalApproved;
    private Integer totalRejected;
    private Integer totalPending;

    // ═══════════════════════════════════════════════════════════════
    // APPROVAL HISTORY
    // ═══════════════════════════════════════════════════════════════

    private List<ApprovalRecordDTO> approvalHistory;
    private LocalDateTime lastUpdated;

    /**
     * Nested DTO for individual approval record
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalRecordDTO {
        private Long leaveId;
        private String leaveType;
        private String status;               // APPROVED, REJECTED, PENDING, CANCELLED
        private LocalDate startDate;
        private LocalDate endDate;
        private Double daysRequested;
        private String reason;               // Leave reason

        // ═══════════════════════════════════════════════════════════
        // APPROVAL AUDIT TRAIL
        // ═══════════════════════════════════════════════════════════

        private Long approverId;             // User ID of approver
        private String approverName;         // Name of approver (Manager/HR/Admin)
        private String approverRole;         // MANAGER, HR, ADMIN
        private LocalDateTime appliedAt;     // When employee applied
        private LocalDateTime approvedAt;    // When approved/rejected

        // ═══════════════════════════════════════════════════════════
        // DEDUCTION DETAILS (if approved)
        // ═══════════════════════════════════════════════════════════

        private Double carryForwardUsed;     // Days used from carry forward
        private Double compOffUsed;          // Days used from comp-off
        private Double lossOfPayApplied;     // Loss of pay percentage
    }

    /**
     * Get approval summary by manager
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalSummaryByManagerDTO {
        private String managerName;
        private Integer totalApprovedByManager;
        private Integer totalRejectedByManager;
        private Integer averageApprovalTime; // in hours
    }

    /**
     * Get summary by approval status
     */
    public ApprovalSummaryDTO toSummary() {
        ApprovalSummaryDTO summary = new ApprovalSummaryDTO();
        summary.setEmployeeName(employeeName);
        summary.setTotalApproved(totalApproved);
        summary.setTotalRejected(totalRejected);
        summary.setTotalPending(totalPending);
        return summary;
    }

    /**
     * Compact approval summary
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalSummaryDTO {
        private String employeeName;
        private Integer totalApproved;
        private Integer totalRejected;
        private Integer totalPending;
    }
}
