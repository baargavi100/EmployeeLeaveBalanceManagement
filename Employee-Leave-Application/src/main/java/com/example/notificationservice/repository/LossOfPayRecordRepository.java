package com.example.notificationservice.repository;

import com.example.notificationservice.entity.LossOfPayRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LossOfPayRecordRepository
        extends JpaRepository<LossOfPayRecord, Long> {
    // Check if you need to return a List or a single Optional
    Optional<LossOfPayRecord> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    @Query("""
        SELECT COALESCE(SUM(l.lopPercentage), 0)
        FROM LossOfPayRecord l
        WHERE l.employeeId = :employeeId
        AND l.year = :year
    """)
    Double getTotalLossPercentageByEmployeeIdAndYear(
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year
    );
}
