package com.propintel.domain.watch.service;

import com.propintel.domain.investment.calc.model.SourceMeta;
import com.propintel.domain.watch.entity.RuleType;
import com.propintel.domain.watch.entity.WatchItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class WatchDtos {
    private WatchDtos() {}

    public record CreateRequest(@NotNull WatchItem.TargetType targetType, @NotBlank String regionCode,
                                Long complexId, String label, Double areaMin, Double areaMax) {}

    public record UpdateRequest(String label, Double areaMin, Double areaMax, boolean active) {}

    public record RuleView(Long id, RuleType ruleType, String label, double threshold, String thresholdHelp, boolean enabled) {}

    public record RuleUpdate(@NotNull RuleType ruleType, double threshold, boolean enabled) {}

    /** 관심 목록 한 줄에 보여줄 모니터링 지표 */
    public record WatchStatus(
            Long id, WatchItem.TargetType targetType, String regionCode, Long complexId, String label, String areaLabel,
            boolean active, String refMonth, Long currentAvgPrice, Long currentPrice84, int refSample,
            Double change1m, Double change3m, Double change6m, Double change12m,
            Integer volumeRef, Double volumeBaseline, Double volumeRatio, String volumeTrend,
            Long jeonsePrice84, Double jeonseChange3m, Double jeonseRatio,
            int newHighCount, NotableRef latestNewHigh, int dropCount, String flag,
            LocalDate lastDealDate, LocalDateTime lastCollectedAt, LocalDateTime lastEvaluatedAt, SourceMeta source,
            List<String> notes) {}

    public record NotableRef(String complexName, double area, int floor, long price, LocalDate date, Long referencePrice, double diffRate) {}

    public record NewsRef(Long id, String title, String url, LocalDateTime publishedAt, String category, boolean mock) {}

    /** 알림 상세 — 명세의 "어디서 / 얼마나 / 언제부터 / 거래량 / 신고가 / 관련 뉴스" */
    public record AlertDetail(String where, String howMuch, String since, String volume, String newHigh,
                              List<NewsRef> news, String newsNote, String refMonth, String dataSource, String caution) {}

    public record AlertView(Long id, Long watchId, String watchLabel, RuleType ruleType, String ruleLabel,
                            String severity, String title, AlertDetail detail, LocalDateTime triggeredAt, boolean read) {}

    public record EvaluateResult(int watches, int alertsCreated, List<String> errors) {}
}
