package com.propintel.domain.watch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propintel.domain.investment.entity.PolicyRegion;
import com.propintel.domain.investment.service.PolicyService;
import com.propintel.domain.market.repository.ComplexLookupRepository;
import com.propintel.domain.watch.entity.*;
import com.propintel.domain.watch.repository.AlertRepository;
import com.propintel.domain.watch.repository.AlertRuleRepository;
import com.propintel.domain.watch.repository.WatchItemRepository;
import com.propintel.domain.watch.service.WatchDtos.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 관심 지역·단지 등록과 알림 조건 관리 */
@Service
public class WatchService {

    private final WatchItemRepository watchRepo;
    private final AlertRuleRepository ruleRepo;
    private final AlertRepository alertRepo;
    private final PolicyService policyService;
    private final ComplexLookupRepository complexRepo;
    private final MonitoringService monitoring;
    private final ObjectMapper objectMapper;

    public WatchService(WatchItemRepository watchRepo, AlertRuleRepository ruleRepo, AlertRepository alertRepo,
                        PolicyService policyService, ComplexLookupRepository complexRepo,
                        MonitoringService monitoring, ObjectMapper objectMapper) {
        this.watchRepo = watchRepo;
        this.ruleRepo = ruleRepo;
        this.alertRepo = alertRepo;
        this.policyService = policyService;
        this.complexRepo = complexRepo;
        this.monitoring = monitoring;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WatchItem create(CreateRequest r) {
        PolicyRegion region = policyService.region(r.regionCode());
        String label = r.label();
        if (r.targetType() == WatchItem.TargetType.COMPLEX) {
            if (r.complexId() == null) throw new IllegalArgumentException("관심 단지는 complexId 가 필요합니다.");
            var c = complexRepo.findById(r.complexId())
                    .orElseThrow(() -> new EntityNotFoundException("단지 없음: " + r.complexId()));
            if (label == null || label.isBlank()) label = region.shortName() + " " + c.getName();
        } else if (label == null || label.isBlank()) {
            label = region.displayName();
        }
        if (r.areaMin() != null && r.areaMax() != null && r.areaMin() > r.areaMax()) {
            throw new IllegalArgumentException("면적 범위를 확인하세요.");
        }
        WatchItem w = watchRepo.save(new WatchItem(r.targetType(), r.regionCode(),
                r.targetType() == WatchItem.TargetType.COMPLEX ? r.complexId() : null, label, r.areaMin(), r.areaMax()));
        for (RuleType t : RuleType.values()) {
            ruleRepo.save(new AlertRule(w, t, t.defaultThreshold(), true));
        }
        return w;
    }

    @Transactional(readOnly = true)
    public List<WatchStatus> statuses() {
        List<WatchStatus> out = new ArrayList<>();
        for (WatchItem w : watchRepo.findAllByOrderByCreatedAtAsc()) out.add(monitoring.status(w));
        return out;
    }

    @Transactional(readOnly = true)
    public WatchStatus status(Long id) {
        return monitoring.status(get(id));
    }

    @Transactional
    public WatchItem update(Long id, UpdateRequest r) {
        WatchItem w = get(id);
        w.update(r.label() == null || r.label().isBlank() ? w.getLabel() : r.label(), r.areaMin(), r.areaMax(), r.active());
        return w;
    }

    @Transactional
    public void delete(Long id) {
        WatchItem w = get(id);
        alertRepo.deleteByWatchItemId(id);
        ruleRepo.deleteByWatchItemId(id);
        watchRepo.delete(w);
    }

    @Transactional(readOnly = true)
    public List<RuleView> rules(Long id) {
        get(id);
        return ruleRepo.findByWatchItemIdOrderByIdAsc(id).stream().map(WatchService::view).toList();
    }

    @Transactional
    public List<RuleView> updateRules(Long id, List<RuleUpdate> updates) {
        WatchItem w = get(id);
        List<AlertRule> rules = ruleRepo.findByWatchItemIdOrderByIdAsc(id);
        for (RuleUpdate u : updates) {
            if (u.threshold() < 0) throw new IllegalArgumentException("기준값은 0 이상이어야 합니다: " + u.ruleType());
            AlertRule rule = rules.stream().filter(x -> x.getRuleType() == u.ruleType()).findFirst()
                    .orElseGet(() -> ruleRepo.save(new AlertRule(w, u.ruleType(), u.threshold(), u.enabled())));
            rule.update(u.threshold(), u.enabled());
        }
        return ruleRepo.findByWatchItemIdOrderByIdAsc(id).stream().map(WatchService::view).toList();
    }

    @Transactional(readOnly = true)
    public List<AlertView> alerts(int limit) {
        return alertRepo.findRecent(PageRequest.of(0, Math.min(limit, 200))).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return alertRepo.countByReadFalse();
    }

    @Transactional
    public void markRead(Long alertId) {
        alertRepo.findById(alertId).orElseThrow(() -> new EntityNotFoundException("알림 없음: " + alertId)).markRead();
    }

    @Transactional
    public int markAllRead() {
        List<Alert> all = alertRepo.findAll();
        int n = 0;
        for (Alert a : all) if (!a.isRead()) { a.markRead(); n++; }
        return n;
    }

    private WatchItem get(Long id) {
        return watchRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("관심 항목 없음: " + id));
    }

    static RuleView view(AlertRule r) {
        return new RuleView(r.getId(), r.getRuleType(), r.getRuleType().label(), r.getThreshold(),
                r.getRuleType().thresholdHelp(), r.isEnabled());
    }

    AlertView view(Alert a) {
        AlertDetail d;
        try {
            d = objectMapper.readValue(a.getDetailJson(), AlertDetail.class);
        } catch (Exception e) {
            d = null;
        }
        return new AlertView(a.getId(), a.getWatchItem().getId(), a.getWatchItem().getLabel(), a.getRuleType(),
                a.getRuleType().label(), a.getSeverity(), a.getTitle(), d, a.getTriggeredAt(), a.isRead());
    }
}
