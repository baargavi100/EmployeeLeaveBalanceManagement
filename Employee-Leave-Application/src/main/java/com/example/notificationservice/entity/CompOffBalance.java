// ═══════════════════════════════════════════════════════════════════
// FILE: CompOffBalance.java
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
import lombok.Data;

@Entity
@Table(name = "comp_off_balance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "lop_year"}))
@Data
public class CompOffBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "lop_year", nullable = false)  // ✅ Matches H2 column name
    private Integer year;

    @Column(nullable = false)
    private Double earned = 0.0;

    @Column(nullable = false)
    private Double used = 0.0;

    @Column(nullable = false)
    private Double balance = 0.0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void calculateBalance() {
        this.balance = this.earned - this.used;
        this.updatedAt = LocalDateTime.now();
    }

    // Explicit getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Double getEarned() { return earned; }
    public void setEarned(Double earned) { this.earned = earned; }

    public Double getUsed() { return used; }
    public void setUsed(Double used) { this.used = used; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}