package com.propintel.domain.investment.calc.model;

/** 지역 정보 — 규제지역/토지거래허가구역 여부는 시점에 따라 바뀌므로 출처와 함께 관리 */
public record RegionInfo(
        String code,
        String sido,
        String sigungu,
        boolean metro,
        boolean regulated,
        boolean landPermitZone,
        SourceMeta source) {

    public RegionType regionType() {
        if (regulated) return RegionType.REGULATED;
        return metro ? RegionType.METRO_NON_REGULATED : RegionType.NON_METRO;
    }

    public String displayName() {
        return sigungu == null || sigungu.isBlank() ? sido : sido + " " + sigungu;
    }
}
