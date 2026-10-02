package com.propintel.domain.investment.calc;

import com.propintel.domain.investment.calc.model.BorrowerType;
import com.propintel.domain.investment.calc.model.RateType;
import com.propintel.domain.investment.calc.model.RegionInfo;
import com.propintel.domain.investment.calc.model.RegionType;
import com.propintel.domain.investment.calc.model.RepaymentMethod;
import com.propintel.domain.investment.calc.model.SourceMeta;
import com.propintel.domain.investment.calc.model.UsageType;
import com.propintel.domain.investment.calc.policy.DsrRule;
import com.propintel.domain.investment.calc.policy.LoanCapTier;
import com.propintel.domain.investment.calc.policy.LoanRule;
import com.propintel.domain.investment.calc.policy.PolicySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 주택담보대출 예상 한도 = min(LTV 한도, 가격구간별 절대한도, DSR 한도).
 * 실제 금융기관 심사(신용도, 소득 인정 방식, 상품별 조건)와 다를 수 있다.
 */
public final class LoanLimitCalculator {

    private LoanLimitCalculator() {}

    public record Input(
            long price,
            BorrowerType borrowerType,
            UsageType usage,
            long tenantDeposit,
            long annualIncome,
            long existingAnnualDebtService,
            double annualRate,
            int termYears,
            RepaymentMethod method,
            RateType rateType) {}

    public record Result(
            boolean allowed,
            BorrowerType borrowerType,
            String borrowerLabel,
            RegionType regionType,
            String regionTypeLabel,
            double ltvRatio,
            long ltvLimit,
            Long capLimit,
            double dsrLimitRatio,
            double stressRateApplied,
            double dsrTestRate,
            long dsrLimit,
            long maxLoan,
            String bindingConstraint,
            List<String> reasons,
            List<SourceMeta> sources) {}

    public static Result calculate(PolicySnapshot p, RegionInfo region, Input in) {
        RegionType rt = region.regionType();
        List<String> reasons = new ArrayList<>();
        List<SourceMeta> sources = new ArrayList<>();
        sources.add(region.source());

        DsrRule dsr = p.dsrRule(rt);
        sources.add(dsr.source());
        double stress = dsr.appliedStress(in.rateType());
        double testRate = in.annualRate() + stress;

        Optional<LoanRule> ruleOpt = p.loanRule(rt, in.borrowerType());
        if (ruleOpt.isEmpty()) {
            reasons.add("해당 지역·차주 유형의 LTV 규칙 데이터가 없어 대출 가능액을 0원으로 계산했습니다.");
            return blocked(in, rt, dsr, stress, testRate, reasons, sources, 0);
        }
        LoanRule rule = ruleOpt.get();
        sources.add(rule.source());
        if (rule.note() != null && !rule.note().isBlank()) reasons.add(rule.note());

        if (!rule.allowed()) {
            reasons.add(rt.label() + "에서 " + in.borrowerType().label() + "의 주택 구입 목적 주담대는 불가로 계산했습니다.");
            return blocked(in, rt, dsr, stress, testRate, reasons, sources, rule.ltvRatio());
        }
        if (in.usage() != UsageType.OWNER_OCCUPY && rule.requiresMoveIn()) {
            reasons.add("이 지역은 주담대 실행 시 일정 기간 내 전입(실거주) 의무가 있어, "
                    + in.usage().label() + " 목적 매수는 주담대 없이(보증금+자기자본) 계산했습니다.");
            return blocked(in, rt, dsr, stress, testRate, reasons, sources, rule.ltvRatio());
        }

        // 1) LTV 한도 (임대 시 선순위 임차보증금 차감)
        double ltvRaw = in.price() * rule.ltvRatio();
        long ltvLimit = Math.round(ltvRaw);
        if (in.usage() != UsageType.OWNER_OCCUPY && in.tenantDeposit() > 0) {
            ltvLimit = Math.max(0, Math.round(ltvRaw - in.tenantDeposit()));
            reasons.add("임대 목적이므로 LTV 한도에서 임차보증금(선순위)을 차감했습니다.");
        }

        // 2) 가격 구간별 절대 한도
        Long capLimit = null;
        Optional<LoanCapTier> tier = p.capTier(rt, in.price());
        if (tier.isPresent()) {
            capLimit = tier.get().maxAmount();
            sources.add(tier.get().source());
        }

        // 3) DSR 한도
        long dsrLimit = dsrMaxLoan(in.annualIncome(), in.existingAnnualDebtService(), dsr.dsrLimit(),
                testRate, in.termYears(), in.method());
        if (stress > 0) {
            reasons.add(String.format("DSR은 실제 금리 %.2f%%에 스트레스 금리 %.2f%%p를 더한 %.2f%%로 심사한다고 가정했습니다.",
                    in.annualRate() * 100, stress * 100, testRate * 100));
        }

        long max = ltvLimit;
        String binding = "LTV";
        if (capLimit != null && capLimit < max) { max = capLimit; binding = "주택가격별 한도"; }
        if (dsrLimit < max) { max = dsrLimit; binding = "DSR"; }
        max = Math.max(0, max);

        return new Result(true, in.borrowerType(), in.borrowerType().label(), rt, rt.label(),
                rule.ltvRatio(), ltvLimit, capLimit, dsr.dsrLimit(), stress, testRate, dsrLimit,
                max, binding, reasons, PolicySnapshot.distinct(sources));
    }

    private static Result blocked(Input in, RegionType rt, DsrRule dsr, double stress, double testRate,
                                  List<String> reasons, List<SourceMeta> sources, double ltv) {
        return new Result(false, in.borrowerType(), in.borrowerType().label(), rt, rt.label(), ltv,
                0, null, dsr.dsrLimit(), stress, testRate, 0, 0, "규제상 불가", reasons,
                PolicySnapshot.distinct(sources));
    }

    /** DSR 산정용 연간 원리금 (만기일시는 원금/대출기간을 원금상환액으로 간주) */
    public static double dsrAnnualDebtService(double principal, double rate, int termYears, RepaymentMethod method) {
        if (principal <= 0) return 0;
        if (method == RepaymentMethod.BULLET) return principal * rate + principal / termYears;
        return LoanScheduleCalculator.firstYearDebtService(principal, rate, termYears, method);
    }

    /** DSR 한도 내 최대 대출액 (이분 탐색, 10만원 단위 내림) */
    public static long dsrMaxLoan(long annualIncome, long existingDebtService, double dsrLimit,
                                  double testRate, int termYears, RepaymentMethod method) {
        double available = annualIncome * dsrLimit - existingDebtService;
        if (available <= 0 || annualIncome <= 0) return 0;
        double lo = 0, hi = 10_000_000_000.0; // 100억
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            if (dsrAnnualDebtService(mid, testRate, termYears, method) <= available) lo = mid; else hi = mid;
        }
        return (long) Math.floor(lo / 100_000.0) * 100_000L;
    }

    /** DSR 비율 = (기존 연간 원리금 + 신규 연간 원리금) / 연소득 */
    public static double dsrRatio(long annualIncome, long existingDebtService, long newLoan,
                                  double rate, int termYears, RepaymentMethod method) {
        if (annualIncome <= 0) return 0;
        return (existingDebtService + dsrAnnualDebtService(newLoan, rate, termYears, method)) / annualIncome;
    }
}
