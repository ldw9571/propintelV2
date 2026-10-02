package com.propintel.domain.market.service;

import com.propintel.domain.complex.entity.Complex;
import com.propintel.domain.complex.entity.Transaction;
import com.propintel.domain.investment.calc.model.SourceMeta;
import com.propintel.domain.investment.entity.PolicyRegion;
import com.propintel.domain.investment.service.PolicyService;
import com.propintel.domain.market.calc.MarketStatsCalculator;
import com.propintel.domain.market.calc.MarketStatsCalculator.*;
import com.propintel.domain.market.entity.CollectionLog;
import com.propintel.domain.market.entity.RentTransaction;
import com.propintel.domain.market.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/** 지역·단지 단위 시장 지표 (가격·변동률·거래량·전세가·전세가율·신고가·하락거래) */
@Service
@Transactional(readOnly = true)
public class MarketStatsService {

    public static final String SOURCE_NAME = "국토교통부 실거래가 공개시스템 (본 서비스 수집 DB)";
    public static final String SOURCE_URL = "https://rt.molit.go.kr";

    public record MarketSummary(String scopeType, String scopeName, String regionCode, Long complexId,
                                Double areaMin, Double areaMax, Stats stats, SourceMeta source,
                                LocalDateTime lastCollectedAt) {}

    public record TradeView(String kind, String complexName, double area, int floor, long price, long deposit,
                            long monthlyRent, LocalDate date, String contractType) {}

    private final SaleTradeRepository saleRepo;
    private final RentTransactionRepository rentRepo;
    private final ComplexLookupRepository complexRepo;
    private final CollectionLogRepository logRepo;
    private final PolicyService policyService;

    public MarketStatsService(SaleTradeRepository saleRepo, RentTransactionRepository rentRepo,
                              ComplexLookupRepository complexRepo, CollectionLogRepository logRepo,
                              PolicyService policyService) {
        this.saleRepo = saleRepo;
        this.rentRepo = rentRepo;
        this.complexRepo = complexRepo;
        this.logRepo = logRepo;
        this.policyService = policyService;
    }

    public MarketSummary region(String code, Double areaMin, Double areaMax, int months) {
        String name = policyService.findRegion(code).map(PolicyRegion::displayName).orElse(code);
        Options o = new Options(areaMin, areaMax, months, 3, YearMonth.now(), 3, 0.05, 6);
        LocalDate rentFrom = YearMonth.now().minusMonths(months + 3L).atDay(1);
        Stats stats = MarketStatsCalculator.compute(
                saleRepo.findSalesByRegion(code).stream().map(MarketStatsService::row).toList(),
                rentRepo.findByRegionFrom(code, rentFrom).stream().map(MarketStatsService::row).toList(), o);
        return new MarketSummary("REGION", name, code, null, areaMin, areaMax, stats, source(stats),
                logRepo.findTopByRegionCodeOrderByCollectedAtDesc(code).map(CollectionLog::getCollectedAt).orElse(null));
    }

    public MarketSummary complex(Long complexId, Double areaMin, Double areaMax, int months) {
        Complex c = complexRepo.findById(complexId)
                .orElseThrow(() -> new EntityNotFoundException("단지 없음: " + complexId));
        String code = c.getRegion() == null ? null : c.getRegion().getCode();
        Options o = new Options(areaMin, areaMax, months, 1, YearMonth.now(), 3, 0.05, 6);
        LocalDate rentFrom = YearMonth.now().minusMonths(months + 3L).atDay(1);
        Stats stats = MarketStatsCalculator.compute(
                saleRepo.findSalesByComplex(complexId).stream().map(MarketStatsService::row).toList(),
                rentRepo.findByComplexFrom(complexId, rentFrom).stream().map(MarketStatsService::row).toList(), o);
        return new MarketSummary("COMPLEX", c.getName() + " (" + c.getAddress() + ")", code, complexId,
                areaMin, areaMax, stats, source(stats),
                code == null ? null : logRepo.findTopByRegionCodeOrderByCollectedAtDesc(code).map(CollectionLog::getCollectedAt).orElse(null));
    }

    public List<TradeView> complexTrades(Long complexId, int months) {
        LocalDate from = LocalDate.now().minusMonths(months);
        List<TradeView> sales = saleRepo.findSalesByComplex(complexId).stream()
                .filter(t -> !t.getDealDate().isBefore(from))
                .map(t -> new TradeView("매매", t.getComplex().getName(), t.getAreaSqm() == null ? 0 : t.getAreaSqm(),
                        t.getFloor() == null ? 0 : t.getFloor(), t.getPrice(), 0, 0, t.getDealDate(), null))
                .toList();
        List<TradeView> rents = rentRepo.findByComplexFrom(complexId, from).stream()
                .map(r -> new TradeView(r.getRentType() == RentTransaction.RentType.JEONSE ? "전세" : "월세",
                        r.getComplex().getName(), r.getAreaSqm(), r.getFloor(), 0, r.getDeposit(), r.getMonthlyRent(),
                        r.getDealDate(), r.getContractType()))
                .toList();
        return java.util.stream.Stream.concat(sales.stream(), rents.stream())
                .sorted(java.util.Comparator.comparing(TradeView::date).reversed()).toList();
    }

    public List<ComplexOption> complexes(String regionCode, String q) {
        return complexRepo.findTop20ByRegion_CodeAndNameContainingOrderByNameAsc(regionCode, q == null ? "" : q)
                .stream().map(c -> new ComplexOption(c.getId(), c.getName(), c.getAddress(), c.getBuildYear())).toList();
    }

    public record ComplexOption(Long id, String name, String address, Integer buildYear) {}

    private static SourceMeta source(Stats s) {
        return new SourceMeta(SOURCE_NAME, SOURCE_URL, s.lastDealDate(), LocalDate.now(), true,
                "공식 원천 데이터. 계약 후 30일 내 신고되므로 최근 1개월은 불완전할 수 있음");
    }

    static SaleRow row(Transaction t) {
        return new SaleRow(t.getComplex().getId(), t.getComplex().getName(),
                t.getAreaSqm() == null ? 0 : t.getAreaSqm(), t.getFloor() == null ? 0 : t.getFloor(),
                t.getPrice(), t.getDealDate());
    }

    static RentRow row(RentTransaction r) {
        return new RentRow(r.getComplex().getId(), r.getComplex().getName(), r.getAreaSqm(), r.getFloor(),
                r.getDeposit(), r.getMonthlyRent(), r.getDealDate(), r.getContractType());
    }
}
