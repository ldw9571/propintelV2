package com.propintel.domain.investment.calc.policy;

import com.propintel.domain.investment.calc.model.SourceMeta;

/**
 * 누진 구간 (세율표·중개보수 요율표 공용)
 * over < 금액 <= upTo 구간에 rate 적용, cap 은 한도액(원), deduction 은 누진공제액(원)
 */
public record Bracket(
        String tableCode,
        long over,
        Long upTo,
        double rate,
        Long cap,
        long deduction,
        SourceMeta source) {

    public boolean contains(double amount) {
        return amount > over && (upTo == null || amount <= upTo);
    }
}
