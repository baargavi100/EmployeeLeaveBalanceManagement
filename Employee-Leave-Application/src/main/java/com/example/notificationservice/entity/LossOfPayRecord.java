// ═══════════════════════════════════════════════════════════════════
// FILE: LossOfPayRecord.java (FIXED - Removed duplicate fields)
// Location: src/main/java/com/example/notificationservice/entity/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entity: Loss of Pay Record
 * Purpose: Track monthly loss of pay (1% per excess day)
 * Table: loss_of_pay_record
 */
@Entity
@Table(name = "loss_of_pay_record",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "lop_year", "lop_month"}))
public class LossOfPayRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    // Avoid reserved SQL keywords by using explicit column names
    @Column(name = "lop_year", nullable = false)
    private Integer year;

    @Column(name = "lop_month", nullable = false)
    private Integer month;

    /**
     * Number of excess days beyond monthly limit
     */
    @Column(name = "excess_days", nullable = false)
    private Double excessDays = 0.0;

    /**
     * Loss of pay percentage (1% per excess day)
     */
    @Column(name = "loss_percentage", nullable = false)
    private Double lossPercentage = 0.0;

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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Double getExcessDays() {
        return excessDays;
    }

    public void setExcessDays(Double excessDays) {
        this.excessDays = excessDays;
    }

    public Double getLossPercentage() {
        return lossPercentage;
    }

    public void setLossPercentage(Double lossPercentage) {
        this.lossPercentage = lossPercentage;
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
        this.updatedAt = updatedAt;
    }
}