package com.propintel.domain.investment.entity;

import com.propintel.common.source.SourceInfo;
import com.propintel.domain.investment.calc.model.RegionInfo;
import jakarta.persistence.*;

/**
 * 규제 판단용 지역(시군구) — 규제지역·토지거래허가구역 지정 여부와 출처.
 * 기존 region 테이블(실거래 수집용)과 코드(법정동 시군구 5자리)로 연결된다.
 */
@Entity
@Table(name = "policy_region")
public class PolicyRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 40)
    private String sido;

    @Column(length = 60)
    private String sigungu;

    @Column(name = "is_metro", nullable = false)
    private boolean metro;

    @Column(name = "is_regulated", nullable = false)
    private boolean regulated;

    @Column(name = "is_land_permit_zone", nullable = false)
    private boolean landPermitZone;

    /** 국토부 실거래 API 수집 가능 여부 (광역시 전체·기타 지방처럼 대표 코드인 경우 false) */
    @Column(name = "collectable", nullable = false)
    private boolean collectable;

    @Embedded
    private SourceInfo source;

    protected PolicyRegion() {}

    public PolicyRegion(String code, String sido, String sigungu, boolean metro, boolean regulated,
                        boolean landPermitZone, boolean collectable, SourceInfo source) {
        this.code = code;
        this.sido = sido;
        this.sigungu = sigungu;
        this.metro = metro;
        this.regulated = regulated;
        this.landPermitZone = landPermitZone;
        this.collectable = collectable;
        this.source = source;
    }

    public RegionInfo toInfo() {
        return new RegionInfo(code, sido, sigungu, metro, regulated, landPermitZone, source.toMeta());
    }

    public String displayName() {
        return sigungu == null || sigungu.isBlank() ? sido : sido + " " + sigungu;
    }

    /** 뉴스 검색 등에 쓰는 짧은 이름: "성남시 분당구" → "분당구", "영등포구" → "영등포구" */
    public String shortName() {
        if (sigungu == null || sigungu.isBlank()) return sido;
        String[] parts = sigungu.split(" ");
        return parts[parts.length - 1].replaceAll("\\(.*\\)", "");
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getSido() { return sido; }
    public String getSigungu() { return sigungu; }
    public boolean isMetro() { return metro; }
    public boolean isRegulated() { return regulated; }
    public boolean isLandPermitZone() { return landPermitZone; }
    public boolean isCollectable() { return collectable; }
    public SourceInfo getSource() { return source; }
}
