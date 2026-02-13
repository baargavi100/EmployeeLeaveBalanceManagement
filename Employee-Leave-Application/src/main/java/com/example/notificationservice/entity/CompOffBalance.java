// ═══════════════════════════════════════════════════════════════════
// FILE: CompOffBalance.java
// Location: src/main/java/com/example/notificationservice/entity/
// IMPORTANT: CompOff is EARNED (not allocated in leave_allocation)
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

@Entity
@Table(name = "comp_off_balance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "lop_year"}))
public class CompOffBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    // Use non-reserved column name
    @Column(name = "lop_year", nullable = false)
    private Integer year;

    /**
     * CompOff days earned (by working extra hours/holidays)
     */
    @Column(name = "earned", nullable = false)
    private Double earned = 0.0;

    /**
     * CompOff days used
     */
    @Column(name = "used", nullable = false)
    private Double used = 0.0;

    /**
     * Balance = earned - used
     */
    @Column(name = "balance", nullable = false)
    private Double balance = 0.0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Calculate balance automatically
     * Balance cannot be negative
     */
    public void calculateBalance() {
        this.balance = Math.max(this.earned - this.used, 0.0);
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateBalance();
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

    public Double getEarned() {
        return earned;
    }

    public void setEarned(Double earned) {
        this.earned = earned;
        calculateBalance();
    }

    public Double getUsed() {
        return used;
    }

    public void setUsed(Double used) {
        this.used = used;
        calculateBalance();
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}