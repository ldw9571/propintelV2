package com.propintel.domain.investment.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.propintel.domain.investment.calc.LoanScheduleCalculator;
import com.propintel.domain.investment.calc.model.RateType;
import com.propintel.domain.investment.calc.model.RepaymentMethod;
import com.propintel.domain.investment.calc.model.SourceMeta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 투자 분석 모듈의 요청/응답 DTO 모음 */
public final class InvestmentDtos {
    private InvestmentDtos() {}

    public record SaveRequest(String title, String memo, @Valid @NotNull AnalysisRequest input) {}

    public record SavedSummary(Long id, String title, String apartmentName, String regionCode, long price,
                               int holdingYears, LocalDate policyAsOf, LocalDateTime createdAt) {}

    public record SavedDetail(SavedSummary summary, String memo, JsonNode input, JsonNode result) {}

    public record ScheduleRequest(@NotNull @Positive Long principal,
                                  @DecimalMin("0.0") @DecimalMax("0.3") double annualRate,
                                  @Min(1) @Max(50) int termYears) {}

    public record ScheduleResponse(List<LoanScheduleCalculator.Schedule> schedules) {}

    public record ProfileRequest(@NotNull @PositiveOrZero Long cash,
                                 @PositiveOrZero long monthlyIncome,
                                 @PositiveOrZero long existingLoanBalance,
                                 @PositiveOrZero long existingAnnualDebtService,
                                 @Min(0) @Max(20) int housesOwned,
                                 boolean firstTimeBuyer,
                                 @NotBlank String targetRegionCode,
                                 @DecimalMin("0.0") @DecimalMax("0.3") double loanRate,
                                 @Min(1) @Max(50) int loanTermYears,
                                 @NotNull RepaymentMethod repaymentMethod,
                                 @NotNull RateType rateType) {}

    /** 대시보드 "나의 투자 가능 금액" */
    public record Capacity(
            long cash,
            String targetRegion,
            String borrowerLabel,
            long maxPurchasePrice,
            long expectedLoanAtMaxPrice,
            long acquisitionCostAtMaxPrice,
            long totalInvestable,
            long dsrLoanLimit,
            long monthlyRepaymentCapacity,
            long monthlyPaymentAtMaxPrice,
            String bindingConstraint,
            List<String> notes,
            List<SourceMeta> sources,
            String disclaimer) {}
}
