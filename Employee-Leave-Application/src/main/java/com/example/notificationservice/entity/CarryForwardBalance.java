package com.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity: Carry Forward Balance
 * Purpose: Store common carry forward pool (not tied to leave category)
 * Table: carry_forward_balance
 */
@Entity
@Table(name = "carry_forward_balance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarryForwardBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Column(name = "total_carried_forward", nullable = false)
    private Double totalCarriedForward = 0.0;

    @Column(name = "total_used", nullable = false)
    private Double totalUsed = 0.0;

    @Column(name = "remaining", nullable = false)
    private Double remaining = 0.0;

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
}