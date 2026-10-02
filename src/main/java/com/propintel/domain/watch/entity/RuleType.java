package com.propintel.domain.watch.entity;

/** 사용자가 켜고 끄고 기준값을 바꿀 수 있는 알림 조건 */
public enum RuleType {
    PRICE_UP_1M("최근 1개월 가격 상승", 0.05, "비율(0.05 = 5%) 이상 상승"),
    PRICE_UP_3M("최근 3개월 가격 상승", 0.10, "비율(0.10 = 10%) 이상 상승"),
    PRICE_DOWN_1M("최근 1개월 가격 하락", 0.05, "비율(0.05 = 5%) 이상 하락"),
    PRICE_DOWN_3M("최근 3개월 가격 하락", 0.10, "비율(0.10 = 10%) 이상 하락"),
    VOLUME_SURGE("거래량 급증", 2.0, "직전 6개월 평균 대비 배수 이상"),
    VOLUME_DROP("거래량 급감", 0.5, "직전 6개월 평균 대비 배수 이하"),
    NEW_HIGH("신고가 발생", 1, "최근 3개월 신고가 건수 이상"),
    ABOVE_AVERAGE("기간 평균 대비 크게 상승", 0.10, "최근 12개월 평균보다 비율 이상 높음"),
    IMPORTANT_NEWS("중요 뉴스", 1, "최근 3일 재개발·교통·공급·정책 뉴스 건수 이상");

    private final String label;
    private final double defaultThreshold;
    private final String thresholdHelp;

    RuleType(String label, double defaultThreshold, String thresholdHelp) {
        this.label = label;
        this.defaultThreshold = defaultThreshold;
        this.thresholdHelp = thresholdHelp;
    }

    public String label() { return label; }
    public double defaultThreshold() { return defaultThreshold; }
    public String thresholdHelp() { return thresholdHelp; }
}
