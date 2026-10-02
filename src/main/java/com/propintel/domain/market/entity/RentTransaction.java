package com.propintel.domain.market.entity;

import com.propintel.domain.complex.entity.Complex;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 아파트 전월세 실거래 (국토교통부 전월세 실거래 API).
 * 기존 transaction 테이블은 매매 전용 쿼리(평균가 등)에서 type 구분 없이 price 를 쓰므로,
 * 기존 기능에 영향을 주지 않도록 별도 테이블에 저장한다.
 */
@Entity
@Table(name = "rent_transaction",
        uniqueConstraints = @UniqueConstraint(name = "uk_rent_dedupe",
                columnNames = {"complex_id", "deal_date", "floor", "area_sqm", "deposit", "monthly_rent"}),
        indexes = {@Index(name = "idx_rent_region_date", columnList = "region_code, deal_date")})
public class RentTransaction {

    public enum RentType { JEONSE, MONTHLY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", nullable = false)
    private Complex complex;

    @Column(name = "region_code", nullable = false, length = 20) private String regionCode;
    @Enumerated(EnumType.STRING) @Column(name = "rent_type", nullable = false, length = 10) private RentType rentType;
    @Column(name = "area_sqm", nullable = false) private double areaSqm;
    @Column(nullable = false) private int floor;
    /** 보증금 (원) */
    @Column(nullable = false) private long deposit;
    /** 월세 (원), 전세는 0 */
    @Column(name = "monthly_rent", nullable = false) private long monthlyRent;
    @Column(name = "deal_date", nullable = false) private LocalDate dealDate;
    /** 신규 / 갱신 */
    @Column(name = "contract_type", length = 10) private String contractType;
    @Column(name = "contract_term", length = 30) private String contractTerm;
    @Column(name = "pre_deposit") private Long preDeposit;
    @Column(name = "pre_monthly_rent") private Long preMonthlyRent;
    @Column(name = "source_name", nullable = false, length = 100) private String sourceName;
    @Column(name = "collected_at", nullable = false) private LocalDateTime collectedAt;

    protected RentTransaction() {}

    public RentTransaction(Complex complex, String regionCode, double areaSqm, int floor, long deposit, long monthlyRent,
                           LocalDate dealDate, String contractType, String contractTerm, Long preDeposit,
                           Long preMonthlyRent) {
        this.complex = complex;
        this.regionCode = regionCode;
        this.rentType = monthlyRent > 0 ? RentType.MONTHLY : RentType.JEONSE;
        this.areaSqm = areaSqm;
        this.floor = floor;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.dealDate = dealDate;
        this.contractType = contractType;
        this.contractTerm = contractTerm;
        this.preDeposit = preDeposit;
        this.preMonthlyRent = preMonthlyRent;
        this.sourceName = "국토교통부 아파트 전월세 실거래가";
        this.collectedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Complex getComplex() { return complex; }
    public String getRegionCode() { return regionCode; }
    public RentType getRentType() { return rentType; }
    public double getAreaSqm() { return areaSqm; }
    public int getFloor() { return floor; }
    public long getDeposit() { return deposit; }
    public long getMonthlyRent() { return monthlyRent; }
    public LocalDate getDealDate() { return dealDate; }
    public String getContractType() { return contractType; }
    public String getContractTerm() { return contractTerm; }
    public Long getPreDeposit() { return preDeposit; }
    public Long getPreMonthlyRent() { return preMonthlyRent; }
    public String getSourceName() { return sourceName; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
}
