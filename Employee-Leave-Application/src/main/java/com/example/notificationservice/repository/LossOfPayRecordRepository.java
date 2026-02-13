// ═══════════════════════════════════════════════════════════════════
// FILE: LossOfPayRecordRepository.java
// Location: src/main/java/com/example/notificationservice/repository/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.repository;

import com.example.notificationservice.entity.LossOfPayRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LossOfPayRecordRepository extends JpaRepository<LossOfPayRecord, Long> {

    /**
     * Find LOP record for specific employee, year, and month
     */
    Optional<LossOfPayRecord> findByEmployeeIdAndYearAndMonth(
            Long employeeId, Integer year, Integer month);

    /**
     * Find all LOP records for employee in year
     */
    List<LossOfPayRecord> findByEmployeeIdAndYear(Long employeeId, Integer year);

    /**
     * Find all LOP records for employee (all years)
     */
    List<LossOfPayRecord> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);

    /**
     * Get total accumulated LOP percentage for employee in year
     */
    @Query("SELECT COALESCE(SUM(lop.lossPercentage), 0.0) " +
            "FROM LossOfPayRecord lop " +
            "WHERE lop.employeeId = :employeeId " +
            "AND lop.year = :year")
    Double getTotalLossPercentageByEmployeeIdAndYear(@Param("employeeId") Long employeeId,
                                                     @Param("year") Integer year);

    /**
     * Check if LOP record exists for employee in specific month/year
     */
    boolean existsByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    /**
     * Delete LOP record for specific month
     */
    void deleteByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);
}