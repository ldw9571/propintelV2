package com.propintel.domain.investment.dto;

import com.propintel.domain.investment.calc.InvestmentAnalyzer;
import com.propintel.domain.investment.calc.model.RateType;
import com.propintel.domain.investment.calc.model.RepaymentMethod;
import com.propintel.domain.investment.calc.model.UsageType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * 투자 분석 요청. 금액은 원 단위, 비율은 소수(0.04 = 4%)로 받는다.
 */
public record AnalysisRequest(
        // 나의 자금
        @NotNull @PositiveOrZero Long cash,
        @PositiveOrZero long monthlyIncome,
        @PositiveOrZero long existingLoanBalance,
        @PositiveOrZero long existingAnnualDebtService,
        // 매물
        @NotBlank String regionCode,
        @Size(max = 200) String apartmentName,
        @NotNull @Positive Long price,
        @Positive double exclusiveArea,
        @Min(0) @Max(20) int housesOwned,
        boolean firstTimeBuyer,
        boolean willSellExistingHome,
        // 활용 방식
        @NotNull UsageType usage,
        @PositiveOrZero long jeonseDeposit,
        @PositiveOrZero long rentDeposit,
        @PositiveOrZero long monthlyRent,
        @PositiveOrZero long ownerMonthlyCost,
        // 대출
        @PositiveOrZero Long desiredLoanAmount,
        @DecimalMin("0.0") @DecimalMax("0.3") double loanRate,
        @Min(1) @Max(50) int loanTermYears,
        @NotNull RepaymentMethod repaymentMethod,
        @NotNull RateType rateType,
        // 시나리오
        @Min(1) @Max(10) int holdingYears,
        @DecimalMin("-0.5") @DecimalMax("0.5") double conservativeRate,
        @DecimalMin("-0.5") @DecimalMax("0.5") double baseRate,
        @DecimalMin("-0.5") @DecimalMax("0.5") double optimisticRate,
        // 선택
        @DecimalMin("0.1") @DecimalMax("1.0") Double publicPriceRatio,
        @PositiveOrZero long otherAcquisitionCost,
        @Min(0) @Max(10) Integer residenceYears,
        LocalDate policyAsOf) {

    public InvestmentAnalyzer.Input toInput() {
        return new InvestmentAnalyzer.Input(cash, monthlyIncome, existingLoanBalance, existingAnnualDebtService,
                apartmentName, price, exclusiveArea, housesOwned, firstTimeBuyer, willSellExistingHome,
                usage, jeonseDeposit, rentDeposit, monthlyRent, ownerMonthlyCost,
                desiredLoanAmount, loanRate, loanTermYears, repaymentMethod, rateType,
                holdingYears, conservativeRate, baseRate, optimisticRate,
                publicPriceRatio, otherAcquisitionCost, residenceYears);
    }
}
