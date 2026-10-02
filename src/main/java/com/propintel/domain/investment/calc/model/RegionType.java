package com.propintel.domain.investment.calc.model;

/** 대출 규제 적용을 위한 지역 구분 */
public enum RegionType {
    REGULATED("규제지역(투기과열·조정대상)"),
    METRO_NON_REGULATED("수도권 비규제지역"),
    NON_METRO("지방(비수도권)");

    private final String label;
    RegionType(String label) { this.label = label; }
    public String label() { return label; }
}
