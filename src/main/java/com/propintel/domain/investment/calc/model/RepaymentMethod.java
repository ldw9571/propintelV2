package com.propintel.domain.investment.calc.model;

/** 대출 상환 방식 */
public enum RepaymentMethod {
    EQUAL_PAYMENT("원리금균등"),
    EQUAL_PRINCIPAL("원금균등"),
    BULLET("만기일시");

    private final String label;
    RepaymentMethod(String label) { this.label = label; }
    public String label() { return label; }
}
