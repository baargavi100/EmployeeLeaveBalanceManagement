package com.example.notificationservice.service;

import com.example.notificationservice.dto.CarryForwardBalanceResponse;
import com.example.notificationservice.dto.CarryForwardEligibilityResponse;
import com.example.notificationservice.entity.*;
import com.example.notificationservice.enums.LeaveStatus;
import com.example.notificationservice.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CarryForwardService {

    private final EmployeeRepository employeeRepository;
    private final LeaveAllocationRepository allocationRepo;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final CarryForwardBalanceRepository carryForwardBalanceRepository;

    private static final int MAX_CARRY_FORWARD = 10;
    private static final double ELIGIBILITY_LIMIT = 10.0;

    // ═══════════════════════════════════════════════════════════════
    // YEAR-END PROCESSING
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void processYearEndCarryForward(Integer fromYear) {

        List<Employee> employees = employeeRepository.findAll();

        for (Employee employee : employees) {
            processEmployeeCarryForward(employee.getId(), fromYear);
        }
    }

    @Transactional
    public void processEmployeeCarryForward(Long employeeId, Integer fromYear) {

        Integer toYear = fromYear + 1;

        List<LeaveAllocation> currentAllocations =
                allocationRepo.findByEmployeeIdAndYear(employeeId, fromYear);

        double totalAllocated = currentAllocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        Double totalUsed = leaveApplicationRepository
                .getTotalUsedDays(employeeId, LeaveStatus.APPROVED, fromYear);

        if (totalUsed == null) totalUsed = 0.0;

        double yearlyBalance = totalAllocated - totalUsed;

        double carryForward = Math.min(
                Math.max(yearlyBalance, 0),
                MAX_CARRY_FORWARD
        );

        storeCommonCarryForward(employeeId, toYear, carryForward);
        createNextYearAllocations(employeeId, toYear);
    }

    private void storeCommonCarryForward(Long employeeId, Integer year, double carryForward) {

        CarryForwardBalance cfBalance =
                carryForwardBalanceRepository
                        .findByEmployeeIdAndYear(employeeId, year)
                        .orElse(new CarryForwardBalance());

        cfBalance.setEmployeeId(employeeId);
        cfBalance.setYear(year);
        cfBalance.setTotalCarriedForward(carryForward);
        cfBalance.setTotalUsed(0.0);
        cfBalance.setRemaining(carryForward);

        carryForwardBalanceRepository.save(cfBalance);
    }

    private void createNextYearAllocations(Long employeeId, Integer year) {

        String[] categories = {"VACATION", "SICK", "CASUAL", "PERSONAL"};
        double[] allocations = {8.0, 6.0, 6.0, 4.0};

        for (int i = 0; i < categories.length; i++) {

            if (allocationRepo.existsByEmployeeIdAndYearAndLeaveCategory(
                    employeeId, year, categories[i])) {
                continue;
            }

            LeaveAllocation alloc = new LeaveAllocation();
            alloc.setEmployeeId(employeeId);
            alloc.setLeaveCategory(categories[i]);
            alloc.setYear(year);
            alloc.setAllocatedDays(allocations[i]);

            allocationRepo.save(alloc);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET CARRY FORWARD BALANCE
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CarryForwardBalanceResponse getBalance(Long employeeId, Integer year) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        CarryForwardBalance cfBalance =
                carryForwardBalanceRepository
                        .findByEmployeeIdAndYear(employeeId, year)
                        .orElse(null);

        CarryForwardBalanceResponse response =
                new CarryForwardBalanceResponse();

        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setYear(year);

        if (cfBalance != null) {
            response.setTotalCarriedForward(cfBalance.getTotalCarriedForward());
            response.setTotalUsed(cfBalance.getTotalUsed());
            response.setRemaining(cfBalance.getRemaining());
        } else {
            response.setTotalCarriedForward(0.0);
            response.setTotalUsed(0.0);
            response.setRemaining(0.0);
        }

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // CHECK ELIGIBILITY
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CarryForwardEligibilityResponse checkEligibility(Long employeeId, Integer year) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        List<LeaveAllocation> allocations =
                allocationRepo.findByEmployeeIdAndYear(employeeId, year);

        double yearlyAllocated = allocations.stream()
                .mapToDouble(LeaveAllocation::getAllocatedDays)
                .sum();

        Double totalUsed = leaveApplicationRepository
                .getTotalUsedDays(employeeId, LeaveStatus.APPROVED, year);

        if (totalUsed == null) totalUsed = 0.0;

        double balance = yearlyAllocated - totalUsed;

        boolean eligible = balance > 0;
        double eligibleAmount = eligible
                ? Math.min(balance, ELIGIBILITY_LIMIT)
                : 0.0;

        String reason = eligible
                ? "Eligible up to max 5 days"
                : "No remaining balance";

        CarryForwardEligibilityResponse response =
                new CarryForwardEligibilityResponse();

        response.setEmployeeId(employeeId);
        response.setEmployeeName(employee.getName());
        response.setYear(year);
        response.setYearlyAllocated(yearlyAllocated);
        response.setTotalUsed(totalUsed);
        response.setBalance(balance);
        response.setEligible(eligible);
        response.setEligibleAmount(eligibleAmount);
        response.setReason(reason);

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    // USE CARRY FORWARD
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public double useCarryForward(Long employeeId, Integer year, double daysNeeded) {

        CarryForwardBalance cfBalance =
                carryForwardBalanceRepository
                        .findByEmployeeIdAndYear(employeeId, year)
                        .orElse(null);

        if (cfBalance == null || cfBalance.getRemaining() <= 0) {
            return 0.0;
        }

        double available = cfBalance.getRemaining();
        double daysUsed = Math.min(daysNeeded, available);

        cfBalance.setTotalUsed(cfBalance.getTotalUsed() + daysUsed);
        cfBalance.setRemaining(cfBalance.getRemaining() - daysUsed);

        carryForwardBalanceRepository.save(cfBalance);

        return daysUsed;
    }

    // ═══════════════════════════════════════════════════════════════
    // HR - GET ALL BALANCES
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<CarryForwardBalanceResponse> getAllBalances(Integer year) {

        List<CarryForwardBalance> balances =
                carryForwardBalanceRepository.findByYear(year);

        List<CarryForwardBalanceResponse> responses = new ArrayList<>();

        for (CarryForwardBalance cf : balances) {

            Employee employee = employeeRepository
                    .findById(cf.getEmployeeId())
                    .orElse(null);

            if (employee == null) continue;

            CarryForwardBalanceResponse response =
                    new CarryForwardBalanceResponse();

            response.setEmployeeId(cf.getEmployeeId());
            response.setEmployeeName(employee.getName());
            response.setYear(year);
            response.setTotalCarriedForward(cf.getTotalCarriedForward());
            response.setTotalUsed(cf.getTotalUsed());
            response.setRemaining(cf.getRemaining());

            responses.add(response);
        }

        return responses;
    }
}
