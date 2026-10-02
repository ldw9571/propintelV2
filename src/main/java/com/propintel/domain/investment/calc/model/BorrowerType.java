package com.propintel.domain.investment.calc.model;

/** 대출 규제상 차주 유형 (매수 시점 주택 보유 상태 기준) */
public enum BorrowerType {
    FIRST_TIME("생애최초 구입자"),
    NO_HOUSE("무주택자"),
    ONE_HOUSE_DISPOSING("1주택자(기존주택 처분 조건)"),
    ONE_HOUSE("1주택자(추가 구입)"),
    MULTI_HOUSE("2주택 이상 보유자");

    private final String label;
    BorrowerType(String label) { this.label = label; }
    public String label() { return label; }

    public static BorrowerType of(int housesOwned, boolean firstTime, boolean willSellExisting) {
        if (housesOwned <= 0) return firstTime ? FIRST_TIME : NO_HOUSE;
        if (housesOwned == 1) return willSellExisting ? ONE_HOUSE_DISPOSING : ONE_HOUSE;
        return MULTI_HOUSE;
    }
}
