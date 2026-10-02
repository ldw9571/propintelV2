package com.propintel.domain.news.entity;

/** 뉴스 분류 — 제목·요약문의 키워드로 규칙 기반 분류 */
public enum NewsCategory {
    REDEVELOPMENT("재건축·재개발·정비사업"),
    TRANSPORT("교통·개발"),
    SUPPLY("입주·분양·공급"),
    POLICY("정책·규제·세금"),
    RATE("금리·대출"),
    REGION("지역 일반"),
    OTHER("기타");

    private final String label;
    NewsCategory(String label) { this.label = label; }
    public String label() { return label; }
}
