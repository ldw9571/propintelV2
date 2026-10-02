package com.propintel.domain.investment.calc.model;

/** 매수 후 활용 방식 */
public enum UsageType {
    OWNER_OCCUPY("실거주"),
    JEONSE("전세 임대"),
    MONTHLY_RENT("월세 임대");

    private final String label;
    UsageType(String label) { this.label = label; }
    public String label() { return label; }
}
