package com.example.employeeLeaveApplication.component;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.employeeLeaveApplication.service.LossOfPayService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final LossOfPayService lossOfPayService;

    @Override
    public void run(String... args) throws Exception {
        // Seed a sample LOP record so H2 shows data for testing
        Long sampleEmployee = 1001L;
        int year = java.time.LocalDate.now().getYear();
        int month = java.time.LocalDate.now().getMonthValue();

        // Apply 2 excess days => 2% LOP
        lossOfPayService.applyLossOfPay(sampleEmployee, year, month, 2.0);
    }
}
