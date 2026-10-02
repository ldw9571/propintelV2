package com.propintel.domain.investment.calc.model;

import java.time.LocalDate;

/**
 * 모든 정책·시장 데이터에 붙는 출처 정보.
 * verified=false 인 데이터는 화면에서 "미검증"으로 표시되고, 사실로 단정하지 않는다.
 */
public record SourceMeta(
        String sourceName,
        String sourceUrl,
        LocalDate baseDate,
        LocalDate updatedAt,
        boolean verified,
        String note) {

    public static SourceMeta userInput() {
        return new SourceMeta("사용자 입력", null, null, null, false, "사용자가 직접 입력한 값");
    }
}
