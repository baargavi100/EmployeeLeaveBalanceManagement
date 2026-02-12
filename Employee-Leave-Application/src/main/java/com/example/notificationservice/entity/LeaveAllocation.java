package com.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity: Leave Allocation
 * Purpose: Store leave allocations by category
 * Table: leave_allocation
 * NOTE: Carry forward is NO LONGER stored here - it's in carry_forward_balance table
 */
@Entity
@Table(name = "leave_allocation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "leave_category", nullable = false)
    private String leaveCategory;

    @Column(name = "leave_year", nullable = false)
    private Integer year;

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

    // ============================================================
    // BACKWARD COMPATIBILITY METHODS
    // These methods exist for compatibility with old code
    // Carry forward is now stored in carry_forward_balance table
    // ============================================================

    /**
     * Legacy method - always returns 0.0
     * Carry forward is now in carry_forward_balance table
     */
    public Double getCarriedForwardDays() {
        return 0.0;
    }

    /**
     * Legacy method - does nothing
     * Carry forward is now in carry_forward_balance table
     */
    public void setCarriedForwardDays(Double days) {
        // Intentionally does nothing - carry forward is stored separately
    }
}