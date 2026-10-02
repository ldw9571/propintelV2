package com.propintel.domain.investment.entity;

import com.propintel.common.source.SourceInfo;
import com.propintel.domain.investment.calc.policy.Bracket;
import jakarta.persistence.*;

import java.time.LocalDate;

/** 누진 구간표 (중개보수·양도세·재산세) */
@Entity
@Table(name = "rate_bracket")
public class RateBracket extends EffectiveRecord {

    @Column(name = "table_code", nullable = false, length = 40) private String tableCode;
    @Column(name = "bracket_over", nullable = false) private long bracketOver;
    @Column(name = "bracket_up_to") private Long bracketUpTo;
    @Column(nullable = false) private double rate;
    @Column(name = "cap_amount") private Long capAmount;
    @Column(name = "progressive_deduction", nullable = false) private long progressiveDeduction;

    protected RateBracket() {}

    public RateBracket(String tableCode, long bracketOver, Long bracketUpTo, double rate, Long capAmount,
                       long progressiveDeduction, LocalDate effectiveFrom, SourceInfo source) {
        super(effectiveFrom, source);
        this.tableCode = tableCode;
        this.bracketOver = bracketOver;
        this.bracketUpTo = bracketUpTo;
        this.rate = rate;
        this.capAmount = capAmount;
        this.progressiveDeduction = progressiveDeduction;
    }

    public Bracket toBracket() {
        return new Bracket(tableCode, bracketOver, bracketUpTo, rate, capAmount, progressiveDeduction, source.toMeta());
    }

    public String key() { return tableCode + ":" + bracketOver; }
}
