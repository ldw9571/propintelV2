package com.propintel.domain.investment.entity;

import com.propintel.common.source.SourceInfo;
import com.propintel.domain.investment.calc.model.BorrowerType;
import com.propintel.domain.investment.calc.model.RegionType;
import com.propintel.domain.investment.calc.policy.LoanRule;
import jakarta.persistence.*;

import java.time.LocalDate;

/** LTV 규칙: 지역유형 × 차주유형 */
@Entity
@Table(name = "loan_regulation")
public class LoanRegulation extends EffectiveRecord {

    @Enumerated(EnumType.STRING) @Column(name = "region_type", nullable = false, length = 30)
    private RegionType regionType;

    @Enumerated(EnumType.STRING) @Column(name = "borrower_type", nullable = false, length = 30)
    private BorrowerType borrowerType;

    @Column(nullable = false) private boolean allowed;
    @Column(name = "ltv_ratio", nullable = false) private double ltvRatio;
    @Column(name = "requires_move_in", nullable = false) private boolean requiresMoveIn;

    protected LoanRegulation() {}

    public LoanRegulation(RegionType regionType, BorrowerType borrowerType, boolean allowed, double ltvRatio,
                          boolean requiresMoveIn, LocalDate effectiveFrom, SourceInfo source) {
        super(effectiveFrom, source);
        this.regionType = regionType;
        this.borrowerType = borrowerType;
        this.allowed = allowed;
        this.ltvRatio = ltvRatio;
        this.requiresMoveIn = requiresMoveIn;
    }

    public LoanRule toRule() {
        return new LoanRule(regionType, borrowerType, allowed, ltvRatio, requiresMoveIn, source.getNote(), source.toMeta());
    }

    public String key() { return regionType + ":" + borrowerType; }
}
