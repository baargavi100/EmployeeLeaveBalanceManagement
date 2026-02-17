// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveApprovalSimulationResponse.java
// Location: src/main/java/com/example/notificationservice/dto/
// ═══════════════════════════════════════════════════════════════════

package com.example.employeeLeaveApplication.dto;

import com.example.employeeLeaveApplication.enums.LeaveType;

public class LeaveApprovalSimulationResponse {

    private Long leaveId;
    private Long employeeId;
    private Double requestedDays;
    private LeaveType leaveType;

    // Approval decision
    private Boolean canApproveDirectly;
    private Boolean requiresUserDecision;

    // Excess days info
    private Double excessDays;

    // Available alternatives
    private Boolean canUseCarryForward;
    private Double carryForwardAvailable;

    private Boolean canUseCompOff;
    private Double compOffAvailable;

    private Double lopPercentage;

    // Message to user
    private String message;

    // Getters and Setters

    public Long getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(Long leaveId) {
        this.leaveId = leaveId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Double getRequestedDays() {
        return requestedDays;
    }

    public void setRequestedDays(Double requestedDays) {
        this.requestedDays = requestedDays;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public Boolean getCanApproveDirectly() {
        return canApproveDirectly;
    }

    public void setCanApproveDirectly(Boolean canApproveDirectly) {
        this.canApproveDirectly = canApproveDirectly;
    }

    public Boolean getRequiresUserDecision() {
        return requiresUserDecision;
    }

    public void setRequiresUserDecision(Boolean requiresUserDecision) {
        this.requiresUserDecision = requiresUserDecision;
    }

    public Double getExcessDays() {
        return excessDays;
    }

    public void setExcessDays(Double excessDays) {
        this.excessDays = excessDays;
    }

    public Boolean getCanUseCarryForward() {
        return canUseCarryForward;
    }

    public void setCanUseCarryForward(Boolean canUseCarryForward) {
        this.canUseCarryForward = canUseCarryForward;
    }

    public Double getCarryForwardAvailable() {
        return carryForwardAvailable;
    }

    public void setCarryForwardAvailable(Double carryForwardAvailable) {
        this.carryForwardAvailable = carryForwardAvailable;
    }

    public Boolean getCanUseCompOff() {
        return canUseCompOff;
    }

    public void setCanUseCompOff(Boolean canUseCompOff) {
        this.canUseCompOff = canUseCompOff;
    }

    public Double getCompOffAvailable() {
        return compOffAvailable;
    }

    public void setCompOffAvailable(Double compOffAvailable) {
        this.compOffAvailable = compOffAvailable;
    }

    public Double getLopPercentage() {
        return lopPercentage;
    }

    public void setLopPercentage(Double lopPercentage) {
        this.lopPercentage = lopPercentage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}