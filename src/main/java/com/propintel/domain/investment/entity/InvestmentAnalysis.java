package com.propintel.domain.investment.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 저장한 투자 분석 — 입력·결과를 JSON 문자열로 보관하고, 계산 기준일을 함께 남긴다 */
@Entity
@Table(name = "investment_analysis")
public class InvestmentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200) private String title;
    @Column(name = "apartment_name", length = 200) private String apartmentName;
    @Column(name = "region_code", nullable = false, length = 20) private String regionCode;
    @Column(nullable = false) private long price;
    @Column(name = "holding_years", nullable = false) private int holdingYears;
    @Column(name = "policy_as_of", nullable = false) private LocalDate policyAsOf;
    @Column(name = "input_json", nullable = false, columnDefinition = "TEXT") private String inputJson;
    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT") private String resultJson;
    @Column(columnDefinition = "TEXT") private String memo;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    protected InvestmentAnalysis() {}

    public InvestmentAnalysis(String title, String apartmentName, String regionCode, long price, int holdingYears,
                              LocalDate policyAsOf, String inputJson, String resultJson, String memo) {
        this.title = title;
        this.apartmentName = apartmentName;
        this.regionCode = regionCode;
        this.price = price;
        this.holdingYears = holdingYears;
        this.policyAsOf = policyAsOf;
        this.inputJson = inputJson;
        this.resultJson = resultJson;
        this.memo = memo;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getApartmentName() { return apartmentName; }
    public String getRegionCode() { return regionCode; }
    public long getPrice() { return price; }
    public int getHoldingYears() { return holdingYears; }
    public LocalDate getPolicyAsOf() { return policyAsOf; }
    public String getInputJson() { return inputJson; }
    public String getResultJson() { return resultJson; }
    public String getMemo() { return memo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
