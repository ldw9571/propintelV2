package com.propintel.domain.watch.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "alert_rule", uniqueConstraints = @UniqueConstraint(name = "uk_rule_watch_type", columnNames = {"watch_item_id", "rule_type"}))
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watch_item_id", nullable = false)
    private WatchItem watchItem;

    @Enumerated(EnumType.STRING) @Column(name = "rule_type", nullable = false, length = 30) private RuleType ruleType;
    @Column(nullable = false) private double threshold;
    @Column(nullable = false) private boolean enabled;

    protected AlertRule() {}

    public AlertRule(WatchItem watchItem, RuleType ruleType, double threshold, boolean enabled) {
        this.watchItem = watchItem;
        this.ruleType = ruleType;
        this.threshold = threshold;
        this.enabled = enabled;
    }

    public void update(double threshold, boolean enabled) {
        this.threshold = threshold;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public WatchItem getWatchItem() { return watchItem; }
    public RuleType getRuleType() { return ruleType; }
    public double getThreshold() { return threshold; }
    public boolean isEnabled() { return enabled; }
}
