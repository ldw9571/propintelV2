package com.propintel.common.source;

import com.propintel.domain.investment.calc.model.SourceMeta;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 데이터 신뢰성용 공통 컬럼: 출처 · 기준일 · 업데이트일 · 검증여부 · 메모.
 * 규제·세율·지역지정·뉴스 등 시점에 따라 바뀌는 데이터에 붙인다.
 */
@Embeddable
public class SourceInfo {

    @Column(name = "source_name", nullable = false, length = 300)
    private String sourceName;

    @Column(name = "source_url", length = 600)
    private String sourceUrl;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "note", length = 600)
    private String note;

    protected SourceInfo() {}

    public SourceInfo(String sourceName, String sourceUrl, LocalDate baseDate, boolean verified, String note) {
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.baseDate = baseDate;
        this.verified = verified;
        this.note = note;
        this.updatedAt = LocalDateTime.now();
    }

    public SourceMeta toMeta() {
        return new SourceMeta(sourceName, sourceUrl, baseDate,
                updatedAt == null ? null : updatedAt.toLocalDate(), verified, note);
    }

    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public LocalDate getBaseDate() { return baseDate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isVerified() { return verified; }
    public String getNote() { return note; }
}
