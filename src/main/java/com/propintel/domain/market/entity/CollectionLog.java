package com.propintel.domain.market.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 실거래 수집 이력 — "언제, 어디를, 몇 건" 수집했는지(데이터 업데이트일) 남긴다 */
@Entity
@Table(name = "collection_log", indexes = @Index(name = "idx_collection_region", columnList = "region_code, deal_ym"))
public class CollectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_code", nullable = false, length = 20) private String regionCode;
    @Column(name = "deal_ym", nullable = false, length = 6) private String dealYm;
    @Column(name = "data_type", nullable = false, length = 10) private String dataType; // SALE / RENT
    private int fetched;
    private int saved;
    private int duplicates;
    private int canceled;
    @Column(length = 500) private String error;
    @Column(name = "collected_at", nullable = false) private LocalDateTime collectedAt;

    protected CollectionLog() {}

    public CollectionLog(String regionCode, String dealYm, String dataType, int fetched, int saved,
                         int duplicates, int canceled, String error) {
        this.regionCode = regionCode;
        this.dealYm = dealYm;
        this.dataType = dataType;
        this.fetched = fetched;
        this.saved = saved;
        this.duplicates = duplicates;
        this.canceled = canceled;
        this.error = error == null ? null : (error.length() > 500 ? error.substring(0, 500) : error);
        this.collectedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRegionCode() { return regionCode; }
    public String getDealYm() { return dealYm; }
    public String getDataType() { return dataType; }
    public int getFetched() { return fetched; }
    public int getSaved() { return saved; }
    public int getDuplicates() { return duplicates; }
    public int getCanceled() { return canceled; }
    public String getError() { return error; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
}
