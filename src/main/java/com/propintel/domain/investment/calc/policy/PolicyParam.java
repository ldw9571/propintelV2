package com.propintel.domain.investment.calc.policy;

import com.propintel.domain.investment.calc.model.SourceMeta;

/** 단일 수치 정책 파라미터 (세율, 기준금액 등) */
public record PolicyParam(String key, double value, String description, SourceMeta source) {
}
