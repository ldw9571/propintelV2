package com.propintel.domain.investment.calc;

import com.propintel.domain.investment.calc.model.BorrowerType;
import com.propintel.domain.investment.calc.model.RateType;
import com.propintel.domain.investment.calc.model.RegionInfo;
import com.propintel.domain.investment.calc.model.RepaymentMethod;
import com.propintel.domain.investment.calc.model.SourceMeta;
import com.propintel.domain.investment.calc.model.UsageType;
import com.propintel.domain.investment.calc.policy.PolicySnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 핵심: 투자금·대출·취득비용·보유비용·매도비용·세금을 모두 반영한 1~10년 시뮬레이션.
 * 가격은 예측하지 않고 사용자가 정한 3개 시나리오(연평균 변동률)로만 계산한다.
 */
public final class InvestmentAnalyzer {

    public static final int SIMULATION_YEARS = 10;

    public static final String DISCLAIMER =
            "본 결과는 입력값과 저장된 규제·세율 데이터(기준일·출처 표시)를 바탕으로 한 학습용 추정치입니다. "
            + "실제 대출 한도와 금리는 금융기관 심사(소득 인정 방식, 신용도, 상품 조건)에 따라 달라지고, "
            + "세금은 개인 상황과 법령 개정에 따라 달라집니다. 투자 권유나 매수 추천이 아니며, "
            + "실제 의사결정 전에는 금융기관·세무 전문가에게 확인하세요.";

    private InvestmentAnalyzer() {}

    // ───────────────────────── 입력 ─────────────────────────

    public record Input(
            long cash,
            long monthlyIncome,
            long existingLoanBalance,
            long existingAnnualDebtService,
            String apartmentName,
            long price,
            double exclusiveArea,
            int housesOwned,
            boolean firstTimeBuyer,
            boolean willSellExistingHome,
            UsageType usage,
            long jeonseDeposit,
            long rentDeposit,
            long monthlyRent,
            long ownerMonthlyCost,
            Long desiredLoanAmount,
            double loanRate,
            int loanTermYears,
            RepaymentMethod repaymentMethod,
            RateType rateType,
            int holdingYears,
            double conservativeRate,
            double baseRate,
            double optimisticRate,
            Double publicPriceRatio,
            long otherAcquisitionCost,
            Integer residenceYears) {

        public long tenantDeposit() {
            return switch (usage) {
                case JEONSE -> jeonseDeposit;
                case MONTHLY_RENT -> rentDeposit;
                case OWNER_OCCUPY -> 0;
            };
        }

        public long monthlyRentIncome() {
            return usage == UsageType.MONTHLY_RENT ? monthlyRent : 0;
        }
    }

    // ───────────────────────── 출력 ─────────────────────────

    public record Funding(
            long cash,
            long price,
            long acquisitionCost,
            long tenantDeposit,
            long maxLoan,
            long loanAmount,
            boolean loanAutoSelected,
            long requiredCash,
            long shortfall,
            long surplus,
            double ltvApplied,
            double dsrStress,
            double dsrActual,
            long monthlyPaymentFirst,
            long monthlyRepaymentCapacity,
            long totalInvestable) {}

    public record LoanCompare(
            RepaymentMethod method,
            String label,
            long firstMonthPayment,
            long lastMonthPayment,
            long firstYearInterest,
            long firstYearPrincipal,
            long totalInterest,
            long balanceAtHoldingEnd) {}

    public record YearRow(
            int year,
            long price,
            long loanBalance,
            long tenantDeposit,
            long netEquity,
            long cumulativeInterest,
            long cumulativePrincipal,
            long cumulativeRentIncome,
            long cumulativeHoldingCost,
            long netProfitIfSold,
            Double roiIfSold) {}

    public record Exit(
            int holdingYears,
            long initialInvestment,
            long acquisitionCost,
            long loanAmount,
            long totalInterest,
            long totalPrincipalRepaid,
            long tenantDeposit,
            long totalRentIncome,
            long totalOwnerCost,
            long totalHoldingTax,
            long additionalCashInvested,
            long salePrice,
            long sellingCost,
            TaxCalculators.CapitalGainsTax capitalGainsTax,
            long loanRepayment,
            long depositReturn,
            long finalRecovery,
            long totalInvested,
            long netProfit,
            Double roi,
            Double annualizedReturn,
            Double irr,
            List<Long> cashFlows) {}

    public record Scenario(String key, String label, double annualRate, List<YearRow> years, Exit exit) {}

    public record Result(
            String apartmentName,
            RegionInfo region,
            LocalDate policyAsOf,
            Funding funding,
            LoanLimitCalculator.Result loanLimit,
            AcquisitionCostCalculator.Result acquisition,
            List<LoanCompare> loanComparison,
            LoanScheduleCalculator.Schedule selectedSchedule,
            TaxCalculators.HoldingTax holdingTaxFirstYear,
            List<Scenario> scenarios,
            List<String> warnings,
            List<String> assumptions,
            List<SourceMeta> sources,
            String disclaimer) {}

    // ───────────────────────── 계산 ─────────────────────────

    public static Result analyze(PolicySnapshot p, RegionInfo region, Input in) {
        validate(in);
        List<String> warnings = new ArrayList<>();
        List<String> assumptions = new ArrayList<>();
        List<SourceMeta> sources = new ArrayList<>();
        sources.add(region.source());

        BorrowerType borrower = BorrowerType.of(in.housesOwned(), in.firstTimeBuyer(), in.willSellExistingHome());
        int housesAfter = in.housesOwned() + 1 - (in.housesOwned() >= 1 && in.willSellExistingHome() ? 1 : 0);
        long deposit = in.tenantDeposit();
        long annualIncome = in.monthlyIncome() * 12;

        // 1) 취득비용
        AcquisitionCostCalculator.Result acq = AcquisitionCostCalculator.calculate(p, in.price(), in.exclusiveArea(),
                housesAfter, region.regulated(), in.firstTimeBuyer() && in.housesOwned() == 0,
                in.otherAcquisitionCost());
        sources.add(p.paramMeta("acq.basic.lowRate").source());
        sources.add(p.brackets("BROKERAGE_SALE").get(0).source());

        // 2) 대출 한도
        LoanLimitCalculator.Result limit = LoanLimitCalculator.calculate(p, region, new LoanLimitCalculator.Input(
                in.price(), borrower, in.usage(), deposit, annualIncome, in.existingAnnualDebtService(),
                in.loanRate(), in.loanTermYears(), in.repaymentMethod(), in.rateType()));
        sources.addAll(limit.sources());

        // 3) 대출 금액 결정: 희망액이 없으면 "부족한 만큼만, 한도 내에서"
        long needed = Math.max(0, in.price() + acq.total() - deposit - in.cash());
        boolean auto = in.desiredLoanAmount() == null;
        long loan;
        if (auto) {
            loan = Math.min(roundUp(needed, 1_000_000), limit.maxLoan());
        } else {
            loan = in.desiredLoanAmount();
            if (loan > limit.maxLoan()) {
                warnings.add(String.format("희망 대출금 %,d원이 예상 한도 %,d원을 초과하여 한도까지만 반영했습니다.", loan, limit.maxLoan()));
                loan = limit.maxLoan();
            }
        }
        loan = Math.max(0, loan);

        LoanScheduleCalculator.Schedule schedule = LoanScheduleCalculator.calculate(
                loan, in.loanRate(), in.loanTermYears(), in.repaymentMethod());

        long requiredCash = in.price() + acq.total() - deposit - loan;
        long shortfall = Math.max(0, requiredCash - in.cash());
        long surplus = Math.max(0, in.cash() - requiredCash);
        double dsrStress = LoanLimitCalculator.dsrRatio(annualIncome, in.existingAnnualDebtService(), loan,
                limit.dsrTestRate(), in.loanTermYears(), in.repaymentMethod());
        double dsrActual = LoanLimitCalculator.dsrRatio(annualIncome, in.existingAnnualDebtService(), loan,
                in.loanRate(), in.loanTermYears(), in.repaymentMethod());
        long capacity = Math.max(0, Math.round((annualIncome * limit.dsrLimitRatio() - in.existingAnnualDebtService()) / 12.0));

        Funding funding = new Funding(in.cash(), in.price(), acq.total(), deposit, limit.maxLoan(), loan, auto,
                requiredCash, shortfall, surplus, in.price() > 0 ? (double) loan / in.price() : 0,
                dsrStress, dsrActual, schedule.firstMonthPayment(), capacity,
                in.cash() + limit.maxLoan() + deposit);

        // 4) 상환방식 비교
        List<LoanCompare> compare = new ArrayList<>();
        for (RepaymentMethod m : RepaymentMethod.values()) {
            LoanScheduleCalculator.Schedule s = LoanScheduleCalculator.calculate(loan, in.loanRate(), in.loanTermYears(), m);
            LoanScheduleCalculator.YearRow y1 = s.year(1);
            compare.add(new LoanCompare(m, m.label(), s.firstMonthPayment(), s.lastMonthPayment(),
                    y1.interest(), y1.principal(), s.totalInterest(), s.balanceAfterYear(in.holdingYears())));
        }

        // 5) 보유세
        double ppr = in.publicPriceRatio() != null ? in.publicPriceRatio() : p.param("ptax.publicPriceRatio.default");
        boolean oneHouse = housesAfter == 1;
        TaxCalculators.HoldingTax holdingTax1 = TaxCalculators.holdingTax(p, in.price(), ppr, oneHouse);
        sources.add(p.brackets("PROPERTY_TAX").get(0).source());
        sources.add(p.brackets("CGT").get(0).source());

        // 6) 시나리오별 1~10년
        long acquisitionExpenses = acq.totalTax() + acq.brokerageFee() + acq.miscCost() + acq.otherCost();
        long initialInvestment = in.price() + acq.total() - deposit - loan;
        List<Scenario> scenarios = new ArrayList<>();
        String[][] defs = {{"conservative", "보수적"}, {"base", "기준"}, {"optimistic", "낙관적"}};
        double[] rates = {in.conservativeRate(), in.baseRate(), in.optimisticRate()};
        for (int s = 0; s < 3; s++) {
            List<YearRow> rows = new ArrayList<>();
            Exit chosen = null;
            for (int n = 1; n <= SIMULATION_YEARS; n++) {
                Exit e = exitAt(p, in, region, schedule, acq, acquisitionExpenses, initialInvestment, loan,
                        deposit, housesAfter, ppr, oneHouse, rates[s], n);
                long priceN = Math.round(in.price() * Math.pow(1 + rates[s], n));
                long bal = schedule.balanceAfterYear(n);
                rows.add(new YearRow(n, priceN, bal, deposit, priceN - bal - deposit,
                        schedule.cumulativeInterest(n), schedule.cumulativePrincipal(n),
                        e.totalRentIncome(), e.totalHoldingTax() + e.totalOwnerCost(),
                        e.netProfit(), e.roi()));
                if (n == in.holdingYears()) chosen = e;
            }
            scenarios.add(new Scenario(defs[s][0], defs[s][1], rates[s], rows, chosen));
        }

        // 7) 경고(판단 대신 사실 위주)
        if (!limit.allowed()) warnings.add("현재 조건에서는 주택담보대출이 불가한 것으로 계산되었습니다. 사유는 대출 한도 항목을 확인하세요.");
        if (shortfall > 0) warnings.add(String.format("필요 현금이 보유 현금보다 %,d원 부족합니다.", shortfall));
        if (region.landPermitZone() && in.usage() != UsageType.OWNER_OCCUPY) {
            warnings.add(region.displayName() + "은(는) 토지거래허가구역으로 표시되어 있습니다. 허가구역에서는 실거주 목적 외 매수(전세·월세 끼고 매수)가 제한될 수 있습니다.");
        }
        if (deposit > 0 && in.price() > 0 && (double) deposit / in.price() >= 0.8) {
            warnings.add(String.format("전세가율(보증금/매매가)이 %.0f%%로 높습니다. 매매가가 하락하면 보증금 반환이 어려워질 수 있습니다(역전세·깡통전세 위험).",
                    100.0 * deposit / in.price()));
        }
        if (holdingTax1.comprehensiveTaxPossible()) warnings.add("종합부동산세 대상이 될 수 있으나 이 계산에는 포함되지 않았습니다.");
        Exit baseExit = scenarios.get(1).exit();
        if (baseExit.additionalCashInvested() > 0) {
            warnings.add(String.format("기준 시나리오에서 보유기간 동안 이자·보유세 등으로 추가 현금 %,d원(연평균 %,d원)이 필요합니다.",
                    baseExit.additionalCashInvested(), baseExit.additionalCashInvested() / in.holdingYears()));
        }
        if (scenarios.get(0).exit().netProfit() < 0) {
            warnings.add(String.format("보수적 시나리오(연 %.1f%%)에서는 %d년 후 순손실 %,d원으로 계산됩니다.",
                    in.conservativeRate() * 100, in.holdingYears(), -scenarios.get(0).exit().netProfit()));
        }
        if (in.loanTermYears() < in.holdingYears()) warnings.add("대출기간이 보유기간보다 짧아 보유 중 대출이 모두 상환되는 것으로 계산했습니다.");
        warnings.addAll(acq.notes().stream().filter(s -> s.contains("중과")).toList());

        assumptions.add("대출 금리는 대출기간 내내 고정이라고 가정했습니다(변동금리 위험 미반영).");
        assumptions.add("전세보증금·월세는 보유기간 동안 변하지 않고 공실이 없다고 가정했습니다.");
        assumptions.add("미래 가격은 예측이 아니라 사용자가 입력한 연평균 변동률 시나리오입니다.");
        assumptions.add("임대소득세, 종합부동산세, 다주택 양도세 중과, 재산세 세부담상한은 반영하지 않았습니다.");
        assumptions.add("매도는 보유기간 마지막 해 말에 이루어지고, 매도 중개보수는 해당 시점 가격 기준 법정 상한요율로 계산했습니다.");
        if (in.usage() == UsageType.OWNER_OCCUPY) {
            assumptions.add("실거주의 경우 절약되는 주거비(전·월세 비용)는 수익에 포함하지 않았습니다. 필요하면 비교 대상 전세 비용과 함께 보세요.");
        }
        assumptions.addAll(acq.notes().stream().filter(s -> !s.contains("중과")).toList());
        assumptions.addAll(holdingTax1.notes());

        return new Result(in.apartmentName(), region, p.asOf(), funding, limit, acq, compare, schedule, holdingTax1,
                scenarios, warnings, assumptions, PolicySnapshot.distinct(sources), DISCLAIMER);
    }

    /** n년 보유 후 매도했을 때의 현금흐름·수익 */
    static Exit exitAt(PolicySnapshot p, Input in, RegionInfo region, LoanScheduleCalculator.Schedule schedule,
                       AcquisitionCostCalculator.Result acq, long acquisitionExpenses, long initialInvestment,
                       long loan, long deposit, int housesAfter, double ppr, boolean oneHouse,
                       double growth, int n) {
        double[] flows = new double[n + 1];
        flows[0] = -initialInvestment;
        long rentTotal = 0, ownerCostTotal = 0, holdTaxTotal = 0, additional = 0;
        for (int y = 1; y <= n; y++) {
            long priceStart = Math.round(in.price() * Math.pow(1 + growth, y - 1));
            long holdTax = TaxCalculators.holdingTax(p, priceStart, ppr, oneHouse).total();
            LoanScheduleCalculator.YearRow ly = schedule.year(y);
            long rent = in.monthlyRentIncome() * 12;
            long ownerCost = in.ownerMonthlyCost() * 12;
            long flow = rent - ly.interest() - ly.principal() - holdTax - ownerCost;
            flows[y] = flow;
            rentTotal += rent; ownerCostTotal += ownerCost; holdTaxTotal += holdTax;
            if (flow < 0) additional += -flow;
        }

        long salePrice = Math.round(in.price() * Math.pow(1 + growth, n));
        long sellingCost = AcquisitionCostCalculator.BrokerageCalculator.calculate(p, salePrice).fee();
        int residence = in.residenceYears() != null ? Math.min(in.residenceYears(), n)
                : (in.usage() == UsageType.OWNER_OCCUPY ? n : 0);
        TaxCalculators.CapitalGainsTax cgt = TaxCalculators.capitalGainsTax(p, new TaxCalculators.CapitalGainsInput(
                in.price(), salePrice, acquisitionExpenses, sellingCost, n, residence, housesAfter == 1,
                region.regulated()));
        long balance = schedule.balanceAfterYear(n);
        long finalRecovery = salePrice - sellingCost - cgt.total() - balance - deposit;
        flows[n] += finalRecovery;

        long totalInvested = Math.max(0, initialInvestment) + additional;
        double sum = 0;
        for (double f : flows) sum += f;
        long netProfit = Math.round(sum);
        Double roi = totalInvested > 0 ? (double) netProfit / totalInvested : null;
        Double annualized = (roi != null && roi > -1) ? Math.pow(1 + roi, 1.0 / n) - 1 : null;
        Double irr = Irr.compute(flows);

        List<Long> cf = new ArrayList<>();
        for (double f : flows) cf.add(Math.round(f));

        return new Exit(n, initialInvestment, acq.total(), loan, schedule.cumulativeInterest(n),
                schedule.cumulativePrincipal(n), deposit, rentTotal, ownerCostTotal, holdTaxTotal, additional,
                salePrice, sellingCost, cgt, balance, deposit, finalRecovery, totalInvested, netProfit,
                roi, annualized, irr, cf);
    }

    private static long roundUp(long v, long unit) {
        return ((v + unit - 1) / unit) * unit;
    }

    private static void validate(Input in) {
        if (in.price() <= 0) throw new IllegalArgumentException("매입 희망 가격을 입력하세요.");
        if (in.cash() < 0) throw new IllegalArgumentException("보유 현금은 0 이상이어야 합니다.");
        if (in.holdingYears() < 1 || in.holdingYears() > SIMULATION_YEARS)
            throw new IllegalArgumentException("보유 기간은 1~10년으로 입력하세요.");
        if (in.tenantDeposit() >= in.price() && in.usage() != UsageType.OWNER_OCCUPY)
            throw new IllegalArgumentException("임차보증금이 매매가 이상입니다. 입력값을 확인하세요.");
        for (double r : new double[]{in.conservativeRate(), in.baseRate(), in.optimisticRate()}) {
            if (r < -0.5 || r > 0.5) throw new IllegalArgumentException("가격 변동률은 연 -50% ~ +50% 범위로 입력하세요.");
        }
    }
}
