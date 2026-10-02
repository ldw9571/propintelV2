package com.propintel.domain.investment.calc.policy;

import com.propintel.domain.investment.calc.model.RegionType;
import com.propintel.domain.investment.calc.model.SourceMeta;

/** 주택가격 구간별 주담대 최대 한도 (priceUpTo == null 이면 상한 없음) */
public record LoanCapTier(
        RegionType regionType,
        long priceOver,
        Long priceUpTo,
        long maxAmount,
        SourceMeta source) {

    public boolean matches(long price) {
        return price > priceOver && (priceUpTo == null || price <= priceUpTo);
    }
}
