package com.propintel.domain.watch.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 관심 지역 / 관심 단지(+평형) */
@Entity
@Table(name = "watch_item")
public class WatchItem {

    public enum TargetType { REGION, COMPLEX }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 10) private TargetType targetType;
    @Column(name = "region_code", nullable = false, length = 20) private String regionCode;
    @Column(name = "complex_id") private Long complexId;
    @Column(nullable = false, length = 200) private String label;
    /** 특정 평형만 보고 싶을 때 전용면적 범위(㎡) */
    @Column(name = "area_min") private Double areaMin;
    @Column(name = "area_max") private Double areaMax;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "last_evaluated_at") private LocalDateTime lastEvaluatedAt;

    protected WatchItem() {}

    public WatchItem(TargetType targetType, String regionCode, Long complexId, String label, Double areaMin, Double areaMax) {
        this.targetType = targetType;
        this.regionCode = regionCode;
        this.complexId = complexId;
        this.label = label;
        this.areaMin = areaMin;
        this.areaMax = areaMax;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String label, Double areaMin, Double areaMax, boolean active) {
        this.label = label;
        this.areaMin = areaMin;
        this.areaMax = areaMax;
        this.active = active;
    }

    public void markEvaluated() { this.lastEvaluatedAt = LocalDateTime.now(); }

    public String areaLabel() {
        if (areaMin == null && areaMax == null) return "전체 면적";
        return (areaMin == null ? "" : Math.round(areaMin) + "㎡") + "~" + (areaMax == null ? "" : Math.round(areaMax) + "㎡");
    }

    public Long getId() { return id; }
    public TargetType getTargetType() { return targetType; }
    public String getRegionCode() { return regionCode; }
    public Long getComplexId() { return complexId; }
    public String getLabel() { return label; }
    public Double getAreaMin() { return areaMin; }
    public Double getAreaMax() { return areaMax; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastEvaluatedAt() { return lastEvaluatedAt; }
}
