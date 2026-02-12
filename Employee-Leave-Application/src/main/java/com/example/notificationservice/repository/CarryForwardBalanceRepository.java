package com.example.notificationservice.repository;

import com.example.notificationservice.entity.CarryForwardBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarryForwardBalanceRepository
        extends JpaRepository<CarryForwardBalance, Long> {

    Optional<CarryForwardBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);

    boolean existsByEmployeeIdAndYear(Long employeeId, Integer year);

    // Added for HR - fetch all balances by year
    List<CarryForwardBalance> findByYear(Integer year);
}
