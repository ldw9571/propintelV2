package com.propintel.domain.investment.controller;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.investment.calc.InvestmentAnalyzer;
import com.propintel.domain.investment.dto.AnalysisRequest;
import com.propintel.domain.investment.dto.InvestmentDtos.*;
import com.propintel.domain.investment.entity.InvestorProfile;
import com.propintel.domain.investment.service.CapacityService;
import com.propintel.domain.investment.service.InvestmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/investment")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final CapacityService capacityService;

    public InvestmentController(InvestmentService investmentService, CapacityService capacityService) {
        this.investmentService = investmentService;
        this.capacityService = capacityService;
    }

    /** 투자 분석 계산 (저장 안 함) */
    @PostMapping("/analyze")
    public ApiResponse<InvestmentAnalyzer.Result> analyze(@Valid @RequestBody AnalysisRequest request) {
        return ApiResponse.ok(investmentService.analyze(request));
    }

    /** 대출 원리금 계산기 — 3가지 상환방식 비교 */
    @PostMapping("/loan-schedule")
    public ApiResponse<ScheduleResponse> schedule(@Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(investmentService.schedule(request));
    }

    @PostMapping("/analyses")
    public ApiResponse<SavedSummary> save(@Valid @RequestBody SaveRequest request) {
        return ApiResponse.ok(investmentService.save(request));
    }

    @GetMapping("/analyses")
    public ApiResponse<List<SavedSummary>> list() {
        return ApiResponse.ok(investmentService.list());
    }

    @GetMapping("/analyses/{id}")
    public ApiResponse<SavedDetail> get(@PathVariable Long id) {
        return ApiResponse.ok(investmentService.get(id));
    }

    @DeleteMapping("/analyses/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        investmentService.delete(id);
        return ApiResponse.ok("삭제되었습니다.");
    }

    /** 내 자금 정보 */
    @GetMapping("/profile")
    public ApiResponse<InvestorProfile> profile() {
        return ApiResponse.ok(capacityService.profile());
    }

    @PutMapping("/profile")
    public ApiResponse<InvestorProfile> updateProfile(@Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok(capacityService.update(request));
    }

    /** 나의 투자 가능 금액 */
    @GetMapping("/capacity")
    public ApiResponse<Capacity> capacity() {
        return ApiResponse.ok(capacityService.capacity());
    }
}
