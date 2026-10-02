package com.propintel.domain.investment.calc.policy;

import com.propintel.domain.investment.calc.model.RateType;
import com.propintel.domain.investment.calc.model.RegionType;
import com.propintel.domain.investment.calc.model.SourceMeta;

/** DSR 한도 및 스트레스 금리 규칙 */
public record DsrRule(
        RegionType regionType,
        double dsrLimit,
        double stressRate,
        double variableRatio,
        double mixedRatio,
        double periodicRatio,
        SourceMeta source) {

    /** 금리유형별로 실제 가산되는 스트레스 금리 */
    public double appliedStress(RateType rateType) {
        return switch (rateType) {
            case VARIABLE -> stressRate * variableRatio;
            case MIXED -> stressRate * mixedRatio;
            case PERIODIC -> stressRate * periodicRatio;
        };
    }
}
