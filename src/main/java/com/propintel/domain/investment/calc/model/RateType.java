package com.propintel.domain.investment.calc.model;

/** 금리 유형 — 스트레스 DSR 가산금리 반영 비율이 달라진다 */
public enum RateType {
    VARIABLE("변동형"),
    MIXED("혼합형"),
    PERIODIC("주기형");

    private final String label;
    RateType(String label) { this.label = label; }
    public String label() { return label; }
}
