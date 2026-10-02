package com.propintel.domain.report.controller;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.report.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @GetMapping("/region/{code}")
    public ApiResponse<?> analyzeRegion(@PathVariable String code) {
        return ApiResponse.ok(aiAnalysisService.analyzeRegion(code));
    }

    @GetMapping("/newlyweds")
    public ApiResponse<?> recommendForNewlyweds(
            @RequestParam(defaultValue = "50000") int budget,
            @RequestParam(defaultValue = "전세") String preference) {
        return ApiResponse.ok(aiAnalysisService.recommendForNewlyweds(budget, preference));
    }

    @GetMapping("/ranking")
    public ApiResponse<?> rankAllRegions() {
        return ApiResponse.ok(aiAnalysisService.rankAllRegions());
    }
}

