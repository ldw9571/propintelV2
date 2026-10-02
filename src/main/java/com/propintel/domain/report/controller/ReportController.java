package com.propintel.domain.report.controller;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.report.entity.AiReport;
import com.propintel.domain.report.service.AiReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AiReportService reportService;

    @GetMapping("/complex/{id}")
    public ApiResponse<AiReport> getReport(@PathVariable Long id) {
        return ApiResponse.ok(
                reportService.getLatest(id)
                        .orElseThrow(() -> new IllegalStateException("리포트 없음"))
        );
    }

    @PostMapping("/complex/{id}/generate")
    public ApiResponse<AiReport> generate(@PathVariable Long id) {
        return ApiResponse.ok(reportService.generateReport(id));
    }
}