package com.propintel.domain.investment.entity;

import com.propintel.common.source.SourceInfo;
import com.propintel.domain.investment.calc.policy.PolicyParam;
import jakarta.persistence.*;

import java.time.LocalDate;

/** 단일 수치 파라미터 (세율·기준금액 등) */
@Entity
@Table(name = "policy_parameter")
public class PolicyParameter extends EffectiveRecord {

    @Column(name = "param_key", nullable = false, length = 80) private String paramKey;
    @Column(name = "param_value", nullable = false) private double paramValue;
    @Column(nullable = false, length = 300) private String description;

    protected PolicyParameter() {}

    public PolicyParameter(String paramKey, double paramValue, String description,
                           LocalDate effectiveFrom, SourceInfo source) {
        super(effectiveFrom, source);
        this.paramKey = paramKey;
        this.paramValue = paramValue;
        this.description = description;
    }

    public PolicyParam toParam() {
        return new PolicyParam(paramKey, paramValue, description, source.toMeta());
    }

    public void update(double value, SourceInfo newSource) {
        this.paramValue = value;
        this.source = newSource;
    }

    public String key() { return paramKey; }
    public String getParamKey() { return paramKey; }
    public double getParamValue() { return paramValue; }
    public String getDescription() { return description; }
}
