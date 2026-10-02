package com.propintel.domain.score.service;

import com.propintel.domain.complex.entity.Complex;
import com.propintel.domain.complex.entity.Transaction;
import com.propintel.domain.complex.repository.ComplexRepository;
import com.propintel.domain.complex.repository.TransactionRepository;
import com.propintel.domain.region.entity.RegionStat;
import com.propintel.domain.region.repository.RegionStatRepository;
import com.propintel.domain.score.entity.ComplexScore;
import com.propintel.domain.score.repository.ComplexScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreCalculatorService {

    private final RegionStatRepository   regionStatRepository;
    private final ComplexScoreRepository scoreRepository;
    private final TransactionRepository  transactionRepository;
    private final ComplexRepository      complexRepository;

    @Transactional
    public ComplexScore calculate(Long complexId) {
        Complex complex = complexRepository.findById(complexId)
                .orElseThrow(() -> new IllegalArgumentException("단지 없음: " + complexId));

        log.info("투자 점수 계산: complexId={}, name={}", complexId, complex.getName());

        int supply     = calcSupplyScore(complex);
        int demand     = calcDemandScore(complex);
        int transport  = calcTransportScore(complex);
        int school     = calcSchoolScore(complex);
        int population = calcPopulationScore(complex);

        int total = (int) Math.round(
                supply     * 0.20 +
                        demand     * 0.25 +
                        transport  * 0.20 +
                        school     * 0.20 +
                        population * 0.15
        );

        scoreRepository.deleteByComplexId(complexId);

        return scoreRepository.save(
                ComplexScore.builder()
                        .complex(complex)
                        .supplyScore(supply)
                        .demandScore(demand)
                        .transportScore(transport)
                        .schoolScore(school)
                        .populationScore(population)
                        .totalScore(total)
                        .scoredAt(LocalDate.now())
                        .build()
        );
    }

    public Optional<ComplexScore> getLatestScore(Long complexId) {
        return scoreRepository.findTopByComplexIdOrderByScoredAtDesc(complexId);
    }

    // ── 세부 점수 계산 ──────────────────────────────────────

    private int calcSupplyScore(Complex complex) {
        List<RegionStat> stats = regionStatRepository
                .findTop24ByRegionOrderByStatYearDescStatMonthDesc(complex.getRegion());
        if (stats.isEmpty()) return 50;
        double avg = stats.stream()
                .mapToInt(s -> s.getTradeVolume() == null ? 0 : s.getTradeVolume())
                .average().orElse(0);
        // 거래량 적을수록 공급 부족 → 점수 높음
        if (avg < 100) return 85;
        if (avg < 300) return 65;
        if (avg < 600) return 45;
        return 30;
    }

    private int calcDemandScore(Complex complex) {
        List<Transaction> recent =
                transactionRepository.findRecentByComplex(complex.getId(), 12);
        if (recent.size() < 6) return 50;
        long first  = recent.stream().limit(6).count();
        long second = recent.stream().skip(6).count();
        if (second >= first * 1.5) return 90;
        if (second >= first * 1.2) return 75;
        if (second >= first)       return 60;
        return 40;
    }

    private int calcTransportScore(Complex complex) {
        // TODO: 카카오 로컬 API로 지하철역 거리 계산
        String code = complex.getRegion().getCode();
        if (code.startsWith("11")) return 90; // 서울
        if (code.startsWith("41")) return 70; // 경기
        if (code.startsWith("28")) return 65; // 인천
        if (code.startsWith("26")) return 60; // 부산
        return 50;
    }

    private int calcSchoolScore(Complex complex) {
        // TODO: 학교알리미 API 연동
        return 70;
    }

    private int calcPopulationScore(Complex complex) {
        List<RegionStat> stats = regionStatRepository
                .findTop24ByRegionOrderByStatYearDescStatMonthDesc(complex.getRegion());
        if (stats.size() < 12) return 50;
        Integer recent = stats.get(0).getPopulation();
        Integer before = stats.get(11).getPopulation();
        if (recent == null || before == null || before == 0) return 50;
        double change = (double)(recent - before) / before * 100;
        if (change > 3)  return 95;
        if (change > 1)  return 80;
        if (change > 0)  return 65;
        if (change > -2) return 50;
        return 30;
    }
}