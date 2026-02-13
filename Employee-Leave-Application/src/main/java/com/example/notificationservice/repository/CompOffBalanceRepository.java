// ═══════════════════════════════════════════════════════════════════
// FILE: CompOffBalanceRepository.java
// Location: src/main/java/com/example/notificationservice/repository/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.repository;

import com.example.notificationservice.entity.CompOffBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompOffBalanceRepository extends JpaRepository<CompOffBalance, Long> {

    /**
     * Find comp-off balance for employee in specific year
     */
    Optional<CompOffBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);

    /**
     * Find all comp-off balances for employee (all years)
     */
    List<CompOffBalance> findByEmployeeId(Long employeeId);

    /**
     * Get total available comp-off balance across all years
     */
    @Query("SELECT COALESCE(SUM(c.balance), 0.0) " +
            "FROM CompOffBalance c " +
            "WHERE c.employeeId = :employeeId " +
            "AND c.balance > 0")
    Double getTotalAvailableBalance(@Param("employeeId") Long employeeId);

    /**
     * Check if comp-off exists for employee in year
     */
    boolean existsByEmployeeIdAndYear(Long employeeId, Integer year);

    /**
     * Delete comp-off balance for employee in year
     */
    void deleteByEmployeeIdAndYear(Long employeeId, Integer year);
}