package com.propintel.domain.investment.entity;

import com.propintel.common.source.SourceInfo;
import jakarta.persistence.*;

import java.time.LocalDate;

/** 유효기간(effective_from ~ effective_to)과 출처를 가진 정책 데이터의 공통 부모 */
@MappedSuperclass
public abstract class EffectiveRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "effective_from", nullable = false)
    protected LocalDate effectiveFrom;

    @Column(name = "effective_to")
    protected LocalDate effectiveTo;

    @Embedded
    protected SourceInfo source;

    protected EffectiveRecord() {}

    protected EffectiveRecord(LocalDate effectiveFrom, SourceInfo source) {
        this.effectiveFrom = effectiveFrom;
        this.source = source;
    }

    public Long getId() { return id; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public SourceInfo getSource() { return source; }
}
