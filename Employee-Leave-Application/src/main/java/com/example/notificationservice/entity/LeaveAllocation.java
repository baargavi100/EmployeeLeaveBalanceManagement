// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveAllocation.java
// Location: src/main/java/com/example/notificationservice/entity/
// IMPORTANT: CompOff is NOT allocated here (it's earned separately)
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_allocation",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"employee_id", "leave_category", "leave_year"}
        ))
public class LeaveAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    /**
     * Leave Category: VACATION, SICK, CASUAL, PERSONAL
     * NOTE: COMP_OFF is NOT allocated here (it's earned)
     */
    @Column(name = "leave_category", nullable = false)
    private String leaveCategory;

    @Column(name = "leave_year", nullable = false)
    private Integer year;

    /**
     * Allocated days for this category
     * VACATION = 8, SICK = 6, CASUAL = 6, PERSONAL = 4
     */
    @Column(name = "allocated_days", nullable = false)
    private Double allocatedDays = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════════

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getLeaveCategory() {
        return leaveCategory;
    }

    public void setLeaveCategory(String leaveCategory) {
        this.leaveCategory = leaveCategory;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getAllocatedDays() {
        return allocatedDays;
    }

    public void setAllocatedDays(Double allocatedDays) {
        this.allocatedDays = allocatedDays;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = LocalDateTime.now();
    }
}