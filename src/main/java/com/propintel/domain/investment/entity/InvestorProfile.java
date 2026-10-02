package com.propintel.domain.investment.entity;

import com.propintel.domain.investment.calc.model.RateType;
import com.propintel.domain.investment.calc.model.RepaymentMethod;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 대시보드 "나의 투자 가능 금액" 계산용 내 자금 정보 (개인용 서비스이므로 1행만 사용) */
@Entity
@Table(name = "investor_profile")
public class InvestorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long cash;
    private long monthlyIncome;
    private long existingLoanBalance;
    private long existingAnnualDebtService;
    private int housesOwned;
    private boolean firstTimeBuyer;
    @Column(length = 20) private String targetRegionCode;
    private double loanRate;
    private int loanTermYears;
    @Enumerated(EnumType.STRING) @Column(length = 30) private RepaymentMethod repaymentMethod;
    @Enumerated(EnumType.STRING) @Column(length = 30) private RateType rateType;
    private LocalDateTime updatedAt;

    protected InvestorProfile() {}

    public static InvestorProfile defaults() {
        InvestorProfile p = new InvestorProfile();
        p.cash = 300_000_000L;
        p.monthlyIncome = 7_000_000L;
        p.targetRegionCode = "11560";
        p.loanRate = 0.04;
        p.loanTermYears = 30;
        p.repaymentMethod = RepaymentMethod.EQUAL_PAYMENT;
        p.rateType = RateType.VARIABLE;
        p.updatedAt = LocalDateTime.now();
        return p;
    }

    public void update(long cash, long monthlyIncome, long existingLoanBalance, long existingAnnualDebtService,
                       int housesOwned, boolean firstTimeBuyer, String targetRegionCode, double loanRate,
                       int loanTermYears, RepaymentMethod repaymentMethod, RateType rateType) {
        this.cash = cash;
        this.monthlyIncome = monthlyIncome;
        this.existingLoanBalance = existingLoanBalance;
        this.existingAnnualDebtService = existingAnnualDebtService;
        this.housesOwned = housesOwned;
        this.firstTimeBuyer = firstTimeBuyer;
        this.targetRegionCode = targetRegionCode;
        this.loanRate = loanRate;
        this.loanTermYears = loanTermYears;
        this.repaymentMethod = repaymentMethod;
        this.rateType = rateType;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public long getCash() { return cash; }
    public long getMonthlyIncome() { return monthlyIncome; }
    public long getExistingLoanBalance() { return existingLoanBalance; }
    public long getExistingAnnualDebtService() { return existingAnnualDebtService; }
    public int getHousesOwned() { return housesOwned; }
    public boolean isFirstTimeBuyer() { return firstTimeBuyer; }
    public String getTargetRegionCode() { return targetRegionCode; }
    public double getLoanRate() { return loanRate; }
    public int getLoanTermYears() { return loanTermYears; }
    public RepaymentMethod getRepaymentMethod() { return repaymentMethod; }
    public RateType getRateType() { return rateType; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
