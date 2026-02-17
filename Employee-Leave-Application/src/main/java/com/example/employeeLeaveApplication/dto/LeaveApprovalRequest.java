// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveApprovalRequest.java
// Location: src/main/java/com/example/notificationservice/dto/request/
// ═══════════════════════════════════════════════════════════════════

package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.LeaveStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for approving/rejecting leave
 */
public class LeaveApprovalRequest {

    @NotNull(message = "Leave application ID is required")
    private Long leaveApplicationId;

    @NotNull(message = "Approver ID is required")
    private Long approverId;

    @NotNull(message = "Status is required (APPROVED/REJECTED)")
    private LeaveStatus status;

    private String comments;

    /**
     * For excess leave scenarios:
     * - true: User chose to use CompOff instead of LOP
     * - false: User accepts LOP
     */
    private Boolean useCompOff = false;

    // ═══════════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════════

    public Long getLeaveApplicationId() {
        return leaveApplicationId;
    }

    public void setLeaveApplicationId(Long leaveApplicationId) {
        this.leaveApplicationId = leaveApplicationId;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Boolean getUseCompOff() {
        return useCompOff;
    }

    public void setUseCompOff(Boolean useCompOff) {
        this.useCompOff = useCompOff;
    }
}