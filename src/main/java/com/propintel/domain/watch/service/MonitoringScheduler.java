package com.propintel.domain.watch.service;

import com.propintel.domain.investment.service.PolicyService;
import com.propintel.domain.market.service.TransactionIngestService;
import com.propintel.domain.market.service.TransactionIngestService.DataType;
import com.propintel.domain.news.service.NewsService;
import com.propintel.domain.watch.entity.WatchItem;
import com.propintel.domain.watch.repository.WatchItemRepository;
import com.propintel.infra.molit.MolitRawClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 주기 작업
 * - 매일 06:30: 관심지역 최근 2개월 실거래 수집(키가 있을 때) → 관심지역 뉴스 수집 → 알림 조건 평가
 * - 3시간마다: 관심지역·정책·금리 뉴스 수집
 */
@Component
public class MonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitoringScheduler.class);

    private final WatchItemRepository watchRepo;
    private final TransactionIngestService ingest;
    private final NewsService newsService;
    private final MonitoringService monitoring;
    private final MolitRawClient molit;
    private final PolicyService policyService;

    @Value("${app.monitor.collect-months:2}")
    private int collectMonths;

    public MonitoringScheduler(WatchItemRepository watchRepo, TransactionIngestService ingest, NewsService newsService,
                               MonitoringService monitoring, MolitRawClient molit, PolicyService policyService) {
        this.watchRepo = watchRepo;
        this.ingest = ingest;
        this.newsService = newsService;
        this.monitoring = monitoring;
        this.molit = molit;
        this.policyService = policyService;
    }

    @Scheduled(cron = "${app.monitor.cron:0 30 6 * * *}", zone = "Asia/Seoul")
    public void daily() {
        Set<String> regions = watchedRegions();
        if (molit.isConfigured()) {
            for (String code : regions) {
                try {
                    if (policyService.region(code).isCollectable()) {
                        ingest.collectRecent(code, collectMonths, EnumSet.of(DataType.SALE, DataType.RENT));
                    }
                } catch (RuntimeException e) {
                    log.warn("[스케줄] {} 실거래 수집 실패: {}", code, e.getMessage());
                }
            }
        } else {
            log.info("[스케줄] MOLIT_API_KEY 미설정 — 실거래 수집 생략");
        }
        collectNews(regions);
        monitoring.evaluateAll();
    }

    @Scheduled(cron = "${app.news.cron:0 0 */3 * * *}", zone = "Asia/Seoul")
    public void news() {
        collectNews(watchedRegions());
    }

    private void collectNews(Set<String> regions) {
        for (String code : regions) {
            try {
                newsService.collectRegion(code);
            } catch (RuntimeException e) {
                log.warn("[스케줄] {} 뉴스 수집 실패: {}", code, e.getMessage());
            }
        }
        try {
            newsService.collectNational();
        } catch (RuntimeException e) {
            log.warn("[스케줄] 전국 뉴스 수집 실패: {}", e.getMessage());
        }
    }

    private Set<String> watchedRegions() {
        Set<String> s = new LinkedHashSet<>();
        for (WatchItem w : watchRepo.findByActiveTrueOrderByCreatedAtAsc()) s.add(w.getRegionCode());
        return s;
    }
}
