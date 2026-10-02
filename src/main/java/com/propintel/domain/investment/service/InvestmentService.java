package com.propintel.domain.investment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.propintel.domain.investment.calc.InvestmentAnalyzer;
import com.propintel.domain.investment.calc.LoanScheduleCalculator;
import com.propintel.domain.investment.calc.model.RegionInfo;
import com.propintel.domain.investment.calc.model.RepaymentMethod;
import com.propintel.domain.investment.calc.policy.PolicySnapshot;
import com.propintel.domain.investment.dto.AnalysisRequest;
import com.propintel.domain.investment.dto.InvestmentDtos.*;
import com.propintel.domain.investment.entity.InvestmentAnalysis;
import com.propintel.domain.investment.repository.InvestmentAnalysisRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/** Phase 1: 투자금 → 대출 → 원리금 → 취득비용 → 1~10년 시뮬레이션 → 수익 분석 */
@Service
public class InvestmentService {

    private final PolicyService policyService;
    private final InvestmentAnalysisRepository repository;
    private final ObjectMapper objectMapper;

    public InvestmentService(PolicyService policyService, InvestmentAnalysisRepository repository,
                             ObjectMapper objectMapper) {
        this.policyService = policyService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public InvestmentAnalyzer.Result analyze(AnalysisRequest req) {
        PolicySnapshot snapshot = policyService.snapshot(req.policyAsOf());
        RegionInfo region = policyService.regionInfo(req.regionCode());
        return InvestmentAnalyzer.analyze(snapshot, region, req.toInput());
    }

    public ScheduleResponse schedule(ScheduleRequest req) {
        return new ScheduleResponse(Arrays.stream(RepaymentMethod.values())
                .map(m -> LoanScheduleCalculator.calculate(req.principal(), req.annualRate(), req.termYears(), m))
                .toList());
    }

    @Transactional
    public SavedSummary save(SaveRequest req) {
        InvestmentAnalyzer.Result result = analyze(req.input());
        String title = (req.title() == null || req.title().isBlank()) ? defaultTitle(req.input(), result) : req.title();
        InvestmentAnalysis e = repository.save(new InvestmentAnalysis(title, req.input().apartmentName(),
                req.input().regionCode(), req.input().price(), req.input().holdingYears(), result.policyAsOf(),
                toJson(req.input()), toJson(result), req.memo()));
        return summary(e);
    }

    @Transactional(readOnly = true)
    public List<SavedSummary> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public SavedDetail get(Long id) {
        InvestmentAnalysis e = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("저장된 분석이 없습니다: " + id));
        try {
            return new SavedDetail(summary(e), e.getMemo(),
                    objectMapper.readTree(e.getInputJson()), objectMapper.readTree(e.getResultJson()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("저장된 분석 데이터를 읽을 수 없습니다.");
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new EntityNotFoundException("저장된 분석이 없습니다: " + id);
        repository.deleteById(id);
    }

    private SavedSummary summary(InvestmentAnalysis e) {
        return new SavedSummary(e.getId(), e.getTitle(), e.getApartmentName(), e.getRegionCode(), e.getPrice(),
                e.getHoldingYears(), e.getPolicyAsOf(), e.getCreatedAt());
    }

    private String defaultTitle(AnalysisRequest in, InvestmentAnalyzer.Result r) {
        String name = in.apartmentName() == null || in.apartmentName().isBlank() ? "매물" : in.apartmentName();
        return r.region().displayName() + " " + name + " " + Math.round(in.price() / 10_000_000.0) / 10.0 + "억";
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 변환 실패");
        }
    }
}
