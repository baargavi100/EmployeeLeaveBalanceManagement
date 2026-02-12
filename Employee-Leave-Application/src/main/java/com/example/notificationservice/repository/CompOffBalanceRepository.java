package com.example.notificationservice.repository;

import com.example.notificationservice.entity.CompOffBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompOffBalanceRepository extends JpaRepository<CompOffBalance, Long> {

    /**
     * Find comp-off balance for employee in a year
     */
    Optional<CompOffBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);

    /**
     * Find all comp-off balances for employee
     */
    List<CompOffBalance> findByEmployeeId(Long employeeId);
}