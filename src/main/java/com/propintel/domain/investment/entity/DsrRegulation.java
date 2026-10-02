package com.propintel.domain.investment.entity;

import com.propintel.common.source.SourceInfo;
import com.propintel.domain.investment.calc.model.RegionType;
import com.propintel.domain.investment.calc.policy.DsrRule;
import jakarta.persistence.*;

import java.time.LocalDate;

/** DSR 한도·스트레스 금리 */
@Entity
@Table(name = "dsr_regulation")
public class DsrRegulation extends EffectiveRecord {

    @Enumerated(EnumType.STRING) @Column(name = "region_type", nullable = false, length = 30)
    private RegionType regionType;
    @Column(name = "dsr_limit", nullable = false) private double dsrLimit;
    @Column(name = "stress_rate", nullable = false) private double stressRate;
    @Column(name = "variable_ratio", nullable = false) private double variableRatio;
    @Column(name = "mixed_ratio", nullable = false) private double mixedRatio;
    @Column(name = "periodic_ratio", nullable = false) private double periodicRatio;

    protected DsrRegulation() {}

    public DsrRegulation(RegionType regionType, double dsrLimit, double stressRate, double variableRatio,
                         double mixedRatio, double periodicRatio, LocalDate effectiveFrom, SourceInfo source) {
        super(effectiveFrom, source);
        this.regionType = regionType;
        this.dsrLimit = dsrLimit;
        this.stressRate = stressRate;
        this.variableRatio = variableRatio;
        this.mixedRatio = mixedRatio;
        this.periodicRatio = periodicRatio;
    }

    public DsrRule toRule() {
        return new DsrRule(regionType, dsrLimit, stressRate, variableRatio, mixedRatio, periodicRatio, source.toMeta());
    }

    public String key() { return regionType.name(); }
}
