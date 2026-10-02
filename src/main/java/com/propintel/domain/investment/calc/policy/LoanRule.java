package com.propintel.domain.investment.calc.policy;

import com.propintel.domain.investment.calc.model.BorrowerType;
import com.propintel.domain.investment.calc.model.RegionType;
import com.propintel.domain.investment.calc.model.SourceMeta;

/** 지역유형 × 차주유형별 LTV 규칙 */
public record LoanRule(
        RegionType regionType,
        BorrowerType borrowerType,
        boolean allowed,
        double ltvRatio,
        boolean requiresMoveIn,
        String note,
        SourceMeta source) {
}
