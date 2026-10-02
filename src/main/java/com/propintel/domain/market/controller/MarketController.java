package com.propintel.domain.market.controller;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.market.entity.CollectionLog;
import com.propintel.domain.market.repository.CollectionLogRepository;
import com.propintel.domain.market.service.MarketStatsService;
import com.propintel.domain.market.service.TransactionIngestService;
import com.propintel.domain.market.service.TransactionIngestService.DataType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Phase 2: 아파트/지역 시장 데이터 */
@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MarketStatsService statsService;
    private final TransactionIngestService ingestService;
    private final CollectionLogRepository logRepo;

    public MarketController(MarketStatsService statsService, TransactionIngestService ingestService,
                            CollectionLogRepository logRepo) {
        this.statsService = statsService;
        this.ingestService = ingestService;
        this.logRepo = logRepo;
    }

    /** 지역 시장 지표 (areaMin/areaMax 로 평형 필터) */
    @GetMapping("/regions/{code}/summary")
    public ApiResponse<MarketStatsService.MarketSummary> region(@PathVariable String code,
                                                                @RequestParam(required = false) Double areaMin,
                                                                @RequestParam(required = false) Double areaMax,
                                                                @RequestParam(defaultValue = "24") int months) {
        return ApiResponse.ok(statsService.region(code, areaMin, areaMax, clamp(months)));
    }

    /** 단지 시장 지표 */
    @GetMapping("/complexes/{id}/summary")
    public ApiResponse<MarketStatsService.MarketSummary> complex(@PathVariable Long id,
                                                                 @RequestParam(required = false) Double areaMin,
                                                                 @RequestParam(required = false) Double areaMax,
                                                                 @RequestParam(defaultValue = "24") int months) {
        return ApiResponse.ok(statsService.complex(id, areaMin, areaMax, clamp(months)));
    }

    /** 단지 최근 거래 (매매·전세·월세) */
    @GetMapping("/complexes/{id}/trades")
    public ApiResponse<List<MarketStatsService.TradeView>> trades(@PathVariable Long id,
                                                                  @RequestParam(defaultValue = "12") int months) {
        return ApiResponse.ok(statsService.complexTrades(id, clamp(months)));
    }

    /** 지역 내 단지 검색 (관심 단지 등록용) */
    @GetMapping("/regions/{code}/complexes")
    public ApiResponse<List<MarketStatsService.ComplexOption>> complexes(@PathVariable String code,
                                                                         @RequestParam(required = false) String q) {
        return ApiResponse.ok(statsService.complexes(code, q));
    }

    /** 국토부 실거래 수집 (매매·전월세) — 예: /collect?regionCode=11560&months=12&types=SALE,RENT */
    @PostMapping("/collect")
    public ApiResponse<TransactionIngestService.CollectResult> collect(@RequestParam String regionCode,
                                                                       @RequestParam(defaultValue = "3") int months,
                                                                       @RequestParam(defaultValue = "SALE,RENT") String types) {
        Set<DataType> t = Arrays.stream(types.split(",")).map(String::trim).map(DataType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DataType.class)));
        return ApiResponse.ok(ingestService.collectRecent(regionCode, months, t));
    }

    @GetMapping("/collect/logs")
    public ApiResponse<List<CollectionLog>> logs() {
        return ApiResponse.ok(logRepo.findTop50ByOrderByCollectedAtDesc());
    }

    private static int clamp(int months) {
        return Math.max(3, Math.min(months, 120));
    }
}
