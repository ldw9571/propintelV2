package com.propintel.domain.watch.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 발생한 알림 — 어디서·얼마나·언제부터·거래량·신고가·관련 뉴스를 함께 저장 */
@Entity
@Table(name = "alert", indexes = @Index(name = "idx_alert_triggered", columnList = "triggered_at"))
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watch_item_id", nullable = false)
    private WatchItem watchItem;

    @Enumerated(EnumType.STRING) @Column(name = "rule_type", nullable = false, length = 30) private RuleType ruleType;
    @Column(nullable = false, length = 10) private String severity; // UP / DOWN / INFO
    @Column(nullable = false, length = 300) private String title;
    /** 알림 상세(JSON): where, howMuch, since, volume, newHigh, news[], dataSource */
    @Column(name = "detail_json", nullable = false, columnDefinition = "TEXT") private String detailJson;
    /** 같은 알림이 반복 생성되지 않도록 하는 키 */
    @Column(name = "dedupe_key", nullable = false, unique = true, length = 200) private String dedupeKey;
    @Column(name = "triggered_at", nullable = false) private LocalDateTime triggeredAt;
    @Column(name = "is_read", nullable = false) private boolean read;

    protected Alert() {}

    public Alert(WatchItem watchItem, RuleType ruleType, String severity, String title, String detailJson, String dedupeKey) {
        this.watchItem = watchItem;
        this.ruleType = ruleType;
        this.severity = severity;
        this.title = title.length() > 300 ? title.substring(0, 300) : title;
        this.detailJson = detailJson;
        this.dedupeKey = dedupeKey;
        this.triggeredAt = LocalDateTime.now();
    }

    public void markRead() { this.read = true; }

    public Long getId() { return id; }
    public WatchItem getWatchItem() { return watchItem; }
    public RuleType getRuleType() { return ruleType; }
    public String getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getDetailJson() { return detailJson; }
    public String getDedupeKey() { return dedupeKey; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public boolean isRead() { return read; }
}
