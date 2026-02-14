// ═══════════════════════════════════════════════════════════════════
// FILE: CompOffBalanceRepository.java
// Location: src/main/java/com/example/notificationservice/repository/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.notificationservice.entity.CompOffBalance;

@Repository
public interface CompOffBalanceRepository extends JpaRepository<CompOffBalance, Long> {

    /**
     * Find by employee ID and year (lop_year column)
     */
    Optional<CompOffBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);

    /**
     * Find by employee ID and lop_year (alias for findByEmployeeIdAndYear)
     */
        @Query("SELECT c FROM CompOffBalance c WHERE c.employeeId = :employeeId AND c.year = :year")
        Optional<CompOffBalance> findByEmployeeIdAndLopYear(
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year
        );

    /**
     * Find all comp-off balances for an employee
     */
    List<CompOffBalance> findByEmployeeId(Long employeeId);

    /**
     * Get total available comp-off balance across all years
     */
    @Query("SELECT SUM(c.balance) FROM CompOffBalance c WHERE c.employeeId = :employeeId")
    Double getTotalAvailableBalance(@Param("employeeId") Long employeeId);
}