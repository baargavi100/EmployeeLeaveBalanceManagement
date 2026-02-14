// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveType.java
// Location: src/main/java/com/example/notificationservice/enums/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.enums;

/**
 * Leave Type Enum - ONLY 5 TYPES
 *
 * IMPORTANT:
 * - Carry Forward is NOT a leave type (it's a deduction mechanism)
 * - Loss of Pay is NOT a leave type (it's a deduction mechanism)
 * - COMP_OFF is earned, not allocated
 * - Half-day is a modifier, not a type (use HalfDayType enum)
 */
public enum LeaveType {
    VACATION,   // 8 days/year
    SICK,       // 4 days/year
    CASUAL,     // 6 days/year
    PERSONAL,   // 4 days/year
    COMP_OFF    // Earned (not allocated)
}