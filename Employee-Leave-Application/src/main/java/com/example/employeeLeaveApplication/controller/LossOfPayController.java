package com.example.employeeLeaveApplication.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.employeeLeaveApplication.entity.LossOfPayRecord;
import com.example.employeeLeaveApplication.service.LossOfPayService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/lop")
@RequiredArgsConstructor
public class LossOfPayController {

    private final LossOfPayService lossOfPayService;

    @PostMapping("/apply")
    public ResponseEntity<String> apply(@RequestBody ApplyLopRequest req) {
        lossOfPayService.applyLossOfPay(req.employeeId, req.year, req.month, req.excessDays);
        return ResponseEntity.ok("LOP applied");
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LossOfPayRecord>> listByEmployee(@PathVariable Long employeeId) {
        List<LossOfPayRecord> list = lossOfPayService.getAllForEmployee(employeeId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/employee/{employeeId}/year/{year}")
    public ResponseEntity<List<LossOfPayRecord>> listByEmployeeYear(@PathVariable Long employeeId, @PathVariable Integer year) {
        List<LossOfPayRecord> list = lossOfPayService.getForEmployeeAndYear(employeeId, year);
        return ResponseEntity.ok(list);
    }

    // Simple request DTO
    public static class ApplyLopRequest {
        public Long employeeId;
        public Integer year;
        public Integer month;
        public Double excessDays;
    }
}
