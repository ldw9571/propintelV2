package com.propintel.domain.investment.entity;

import com.propintel.common.source.SourceInfo;
import com.propintel.domain.investment.calc.model.RegionType;
import com.propintel.domain.investment.calc.policy.LoanCapTier;
import jakarta.persistence.*;

import java.time.LocalDate;

/** 주택가격 구간별 주담대 최대 한도 */
@Entity
@Table(name = "loan_amount_cap")
public class LoanAmountCap extends EffectiveRecord {

    @Enumerated(EnumType.STRING) @Column(name = "region_type", nullable = false, length = 30)
    private RegionType regionType;
    @Column(name = "price_over", nullable = false) private long priceOver;
    @Column(name = "price_up_to") private Long priceUpTo;
    @Column(name = "max_amount", nullable = false) private long maxAmount;

    protected LoanAmountCap() {}

    public LoanAmountCap(RegionType regionType, long priceOver, Long priceUpTo, long maxAmount,
                         LocalDate effectiveFrom, SourceInfo source) {
        super(effectiveFrom, source);
        this.regionType = regionType;
        this.priceOver = priceOver;
        this.priceUpTo = priceUpTo;
        this.maxAmount = maxAmount;
    }

    public LoanCapTier toTier() {
        return new LoanCapTier(regionType, priceOver, priceUpTo, maxAmount, source.toMeta());
    }

    public String key() { return regionType + ":" + priceOver; }
}
