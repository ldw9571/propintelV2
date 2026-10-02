package com.propintel.domain.watch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.propintel.common.util.Won;
import com.propintel.domain.market.calc.MarketStatsCalculator.MonthPoint;
import com.propintel.domain.market.calc.MarketStatsCalculator.NotableTrade;
import com.propintel.domain.market.calc.MarketStatsCalculator.Stats;
import com.propintel.domain.market.service.MarketStatsService;
import com.propintel.domain.market.service.MarketStatsService.MarketSummary;
import com.propintel.domain.news.entity.NewsCategory;
import com.propintel.domain.news.service.NewsService;
import com.propintel.domain.watch.entity.*;
import com.propintel.domain.watch.repository.AlertRepository;
import com.propintel.domain.watch.repository.AlertRuleRepository;
import com.propintel.domain.watch.repository.WatchItemRepository;
import com.propintel.domain.watch.service.WatchDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Phase 3: 관심 지역·단지의 가격 변화 모니터링과 급등/급락 알림.
 * 알림에는 "가격이 올랐습니다"가 아니라 어디서·얼마나·언제부터·거래량·신고가·관련 뉴스를 함께 담는다.
 */
@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    private static final Set<NewsCategory> IMPORTANT = EnumSet.of(NewsCategory.REDEVELOPMENT, NewsCategory.TRANSPORT,
            NewsCategory.SUPPLY, NewsCategory.POLICY);

    private final WatchItemRepository watchRepo;
    private final AlertRuleRepository ruleRepo;
    private final AlertRepository alertRepo;
    private final MarketStatsService marketStats;
    private final NewsService newsService;
    private final ObjectMapper objectMapper;

    public MonitoringService(WatchItemRepository watchRepo, AlertRuleRepository ruleRepo, AlertRepository alertRepo,
                             MarketStatsService marketStats, NewsService newsService, ObjectMapper objectMapper) {
        this.watchRepo = watchRepo;
        this.ruleRepo = ruleRepo;
        this.alertRepo = alertRepo;
        this.marketStats = marketStats;
        this.newsService = newsService;
        this.objectMapper = objectMapper;
    }

    // ───────────── 상태 (대시보드·관심 목록) ─────────────

    @Transactional(readOnly = true)
    public WatchStatus status(WatchItem w) {
        MarketSummary m = summary(w);
        Stats s = m.stats();
        NotableTrade h = s.newHighs().isEmpty() ? null : s.newHighs().get(0);
        return new WatchStatus(w.getId(), w.getTargetType(), w.getRegionCode(), w.getComplexId(), w.getLabel(),
                w.areaLabel(), w.isActive(), s.refMonth(), s.currentAvgPrice(), s.currentPrice84(), s.refSampleSize(),
                rate(s, 1), rate(s, 3), rate(s, 6), rate(s, 12),
                s.volume().refCount(), s.volume().baselineAvg(), s.volume().ratio(), s.volume().trend(),
                s.jeonse().price84(), s.jeonse().change3m(), s.jeonse().jeonseRatio(),
                s.newHighs().size(), h == null ? null : new NotableRef(h.complexName(), h.area(), h.floor(), h.price(),
                        h.date(), h.referencePrice(), h.diffRate()),
                s.drops().size(), flag(s), s.lastDealDate(), m.lastCollectedAt(), w.getLastEvaluatedAt(), m.source(),
                s.notes());
    }

    /** 급등/급락 표시 (대시보드 배지) — 기본 기준: 1개월 ±5% 또는 3개월 ±10% */
    static String flag(Stats s) {
        Double c1 = rate(s, 1), c3 = rate(s, 3);
        if ((c1 != null && c1 >= 0.05) || (c3 != null && c3 >= 0.10)) return "급등";
        if ((c1 != null && c1 <= -0.05) || (c3 != null && c3 <= -0.10)) return "급락";
        return null;
    }

    // ───────────── 평가 ─────────────

    @Transactional
    public EvaluateResult evaluateAll() {
        int created = 0;
        List<String> errors = new ArrayList<>();
        List<WatchItem> items = watchRepo.findByActiveTrueOrderByCreatedAtAsc();
        for (WatchItem w : items) {
            try {
                created += evaluate(w);
            } catch (RuntimeException e) {
                log.warn("[모니터링] {} 평가 실패: {}", w.getLabel(), e.getMessage());
                errors.add(w.getLabel() + ": " + e.getMessage());
            }
        }
        log.info("[모니터링] 관심 {}건 평가, 알림 {}건 생성", items.size(), created);
        return new EvaluateResult(items.size(), created, errors);
    }

    @Transactional
    public int evaluate(WatchItem w) {
        MarketSummary m = summary(w);
        Stats s = m.stats();
        List<AlertRule> rules = ruleRepo.findByWatchItemIdOrderByIdAsc(w.getId());
        int created = 0;
        for (AlertRule r : rules) {
            if (!r.isEnabled()) continue;
            Optional<Candidate> c = check(r, w, s);
            if (c.isEmpty()) continue;
            String key = "w" + w.getId() + ":" + r.getRuleType() + ":" + c.get().key();
            if (alertRepo.existsByDedupeKey(key)) continue;
            alertRepo.save(new Alert(w, r.getRuleType(), c.get().severity(), c.get().title(),
                    json(detail(w, s, c.get().howMuch(), c.get().since(), m)), key));
            created++;
        }
        w.markEvaluated();
        return created;
    }

    record Candidate(String severity, String title, String howMuch, String since, String key) {}

    Optional<Candidate> check(AlertRule r, WatchItem w, Stats s) {
        double th = r.getThreshold();
        String where = w.getLabel() + " · " + w.areaLabel();
        String ref = s.refMonth();
        switch (r.getRuleType()) {
            case PRICE_UP_1M, PRICE_UP_3M, PRICE_DOWN_1M, PRICE_DOWN_3M -> {
                int months = r.getRuleType().name().endsWith("1M") ? 1 : 3;
                boolean up = r.getRuleType().name().startsWith("PRICE_UP");
                var ch = s.change(months);
                if (ch == null || ch.rate() == null) return Optional.empty();
                if (up ? ch.rate() < th : ch.rate() > -th) return Optional.empty();
                String how = "84㎡ 환산 평균가 " + Won.format(ch.basePrice84()) + " → " + Won.format(s.currentPrice84())
                        + " (" + Won.pct(ch.rate()) + ")";
                return Optional.of(new Candidate(up ? "UP" : "DOWN",
                        "[" + where + "] 최근 " + months + "개월 가격 " + Won.pct(ch.rate()) + (up ? " 상승" : " 하락"),
                        how, ch.baseMonth() + " 대비 " + ref + " (기준월)", ref));
            }
            case VOLUME_SURGE, VOLUME_DROP -> {
                var v = s.volume();
                if (v.ratio() == null || v.baselineAvg() == null || v.baselineAvg() < 1) return Optional.empty();
                boolean surge = r.getRuleType() == RuleType.VOLUME_SURGE;
                if (surge ? v.ratio() < th : v.ratio() > th) return Optional.empty();
                return Optional.of(new Candidate("INFO",
                        "[" + where + "] 거래량 " + (surge ? "급증" : "급감") + " — 평소의 " + String.format("%.1f", v.ratio()) + "배",
                        "기준월 " + v.refCount() + "건 / 직전 " + v.baselineMonths() + "개월 평균 " + String.format("%.1f", v.baselineAvg()) + "건",
                        ref + " (직전 " + v.baselineMonths() + "개월 평균과 비교)", ref));
            }
            case NEW_HIGH -> {
                if (s.newHighs().size() < Math.max(1, th)) return Optional.empty();
                NotableTrade h = s.newHighs().get(0);
                return Optional.of(new Candidate("UP",
                        "[" + where + "] 신고가 " + s.newHighs().size() + "건 — " + h.complexName() + " " + Math.round(h.area()) + "㎡ " + Won.format(h.price()),
                        "이전 최고 " + Won.format(h.referencePrice()) + " (" + h.referenceDate() + ") 대비 " + Won.pct(h.diffRate()),
                        h.date() + " 거래", h.date() + ":" + h.price()));
            }
            case ABOVE_AVERAGE -> {
                if (ref == null || s.currentPrice84() == null) return Optional.empty();
                List<MonthPoint> ms = s.monthly();
                int refIdx = -1;
                for (int i = 0; i < ms.size(); i++) if (ms.get(i).month().equals(ref)) refIdx = i;
                if (refIdx < 12) return Optional.empty();
                double sum = 0; int n = 0;
                for (int i = refIdx - 12; i < refIdx; i++) {
                    MonthPoint p = ms.get(i);
                    if (p.salePrice84() != null && p.saleCount() >= 1) { sum += p.salePrice84(); n++; }
                }
                if (n < 6) return Optional.empty();
                double avg = sum / n;
                double diff = s.currentPrice84() / avg - 1;
                if (diff < th) return Optional.empty();
                return Optional.of(new Candidate("UP",
                        "[" + where + "] 최근 12개월 평균보다 " + Won.pct(diff) + " 높은 가격",
                        "12개월 평균(84㎡ 환산) " + Won.format(Math.round(avg)) + " → 기준월 " + Won.format(s.currentPrice84()),
                        ms.get(refIdx - 12).month() + "~" + ms.get(refIdx - 1).month() + " 평균 대비", ref));
            }
            case IMPORTANT_NEWS -> {
                LocalDateTime now = LocalDateTime.now();
                var news = newsService.regionNewsBetween(w.getRegionCode(), now.minusDays(3), now).stream()
                        .filter(n -> IMPORTANT.contains(NewsCategory.valueOf(n.category()))).toList();
                if (news.size() < Math.max(1, th)) return Optional.empty();
                return Optional.of(new Candidate("INFO",
                        "[" + w.getLabel() + "] 중요 뉴스 " + news.size() + "건 — " + news.get(0).title(),
                        news.get(0).categoryLabel() + " 관련 보도", "최근 3일", "news" + news.get(0).id()));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    AlertDetail detail(WatchItem w, Stats s, String howMuch, String since, MarketSummary m) {
        var v = s.volume();
        String volume = v.ratio() == null
                ? "기준월 거래 " + v.refCount() + "건 (비교할 직전 거래 부족)"
                : "기준월 거래 " + v.refCount() + "건, 직전 " + v.baselineMonths() + "개월 평균 "
                + String.format("%.1f", v.baselineAvg()) + "건 → " + String.format("%.1f", v.ratio()) + "배 (" + v.trend() + ")";
        String newHigh;
        if (s.newHighs().isEmpty()) newHigh = "최근 3개월 신고가 없음";
        else {
            NotableTrade h = s.newHighs().get(0);
            newHigh = "최근 3개월 신고가 " + s.newHighs().size() + "건 — 최근: " + h.complexName() + " " + Math.round(h.area())
                    + "㎡ " + h.floor() + "층 " + Won.format(h.price()) + " (" + h.date() + ", 이전 최고 " + Won.format(h.referencePrice()) + ")";
        }
        LocalDateTime now = LocalDateTime.now();
        List<NewsRef> news = newsService.regionNewsBetween(w.getRegionCode(), now.minusDays(30), now).stream()
                .limit(3).map(n -> new NewsRef(n.id(), n.title(), n.url(), n.publishedAt(), n.categoryLabel(), n.mock()))
                .toList();
        String newsNote = news.isEmpty() ? "최근 30일 수집된 관련 뉴스가 없습니다." : "최근 30일 관련 뉴스 (가격 변화의 원인이라는 뜻은 아닙니다)";
        return new AlertDetail(w.getLabel() + " · " + w.areaLabel(), howMuch, since, volume, newHigh, news, newsNote,
                s.refMonth(), m.source().sourceName() + " (최근 거래일 " + s.lastDealDate() + ")",
                "실거래 표본이 적으면 소수 거래로 변동률이 크게 나올 수 있습니다. 거래 건수를 함께 확인하세요.");
    }

    MarketSummary summary(WatchItem w) {
        return w.getTargetType() == WatchItem.TargetType.COMPLEX
                ? marketStats.complex(w.getComplexId(), w.getAreaMin(), w.getAreaMax(), 24)
                : marketStats.region(w.getRegionCode(), w.getAreaMin(), w.getAreaMax(), 24);
    }

    static Double rate(Stats s, int months) {
        var c = s.change(months);
        return c == null ? null : c.rate();
    }

    private String json(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
