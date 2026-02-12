package com.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity: Employee Dashboard
 * Purpose: Dashboard view with all leave balances
 * Table: employee_dashboard
 */
@Entity
@Table(name = "employee_dashboard")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, unique = true)
    private Long employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "current_year", nullable = false)
    private Integer currentYear;

    @Column(name = "monthly_allocated", nullable = false)
    private Double monthlyAllocated = 2.0;

    @Column(name = "monthly_used", nullable = false)
    private Double monthlyUsed = 0.0;

    @Column(name = "monthly_balance", nullable = false)
    private Double monthlyBalance = 2.0;

    @Column(name = "yearly_allocated", nullable = false)
    private Double yearlyAllocated = 0.0;

    @Column(name = "yearly_used", nullable = false)
    private Double yearlyUsed = 0.0;

    @Column(name = "yearly_balance", nullable = false)
    private Double yearlyBalance = 0.0;

    @Column(name = "carry_forward_total", nullable = false)
    private Double carryForwardTotal = 0.0;

    @Column(name = "carry_forward_used", nullable = false)
    private Double carryForwardUsed = 0.0;

    @Column(name = "carry_forward_remaining", nullable = false)
    private Double carryForwardRemaining = 0.0;

    @Column(name = "compoff_balance", nullable = false)
    private Double compoffBalance = 0.0;

    @Column(name = "loss_of_pay_percentage", nullable = false)
    private Double lossOfPayPercentage = 0.0;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}