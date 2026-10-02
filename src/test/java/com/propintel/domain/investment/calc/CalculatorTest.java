package com.propintel.domain.investment.calc;

import com.propintel.domain.investment.calc.model.BorrowerType;
import com.propintel.domain.investment.calc.model.RateType;
import com.propintel.domain.investment.calc.model.RepaymentMethod;
import com.propintel.domain.investment.calc.model.UsageType;
import com.propintel.domain.investment.calc.policy.PolicySnapshot;
import org.junit.jupiter.api.Test;

import static com.propintel.domain.investment.calc.TestPolicy.EOK;
import static org.junit.jupiter.api.Assertions.*;

/** 계산 엔진 단위 테스트 — 기대값은 손계산(주석)으로 검증한 값 */
class CalculatorTest {

    private final PolicySnapshot p = TestPolicy.snapshot();

    // ───────── 원리금 ─────────

    @Test
    void 원리금균등_3억_4퍼센트_30년() {
        var s = LoanScheduleCalculator.calculate(3 * EOK, 0.04, 30, RepaymentMethod.EQUAL_PAYMENT);
        // 공식: P·r(1+r)^n / ((1+r)^n − 1) = 1,432,246원
        assertEquals(1_432_246, s.firstMonthPayment(), 1);
        assertEquals(1_432_246, s.lastMonthPayment(), 2);
        assertEquals(215_608_560, s.totalInterest(), 1_000);
        assertEquals(0, s.balanceAfterYear(30));
        assertEquals(30, s.years().size());
    }

    @Test
    void 원금균등_첫달과_총이자() {
        var s = LoanScheduleCalculator.calculate(3 * EOK, 0.04, 30, RepaymentMethod.EQUAL_PRINCIPAL);
        // 첫 달 = 원금 833,333 + 이자 1,000,000
        assertEquals(1_833_333, s.firstMonthPayment(), 1);
        // 총이자 = 월이자(1,000,000) × (360+1)/2
        assertEquals(180_500_000, s.totalInterest(), 10);
        // 10년 후 잔액 = 3억 × (1 − 120/360) = 2억
        assertEquals(2 * EOK, s.balanceAfterYear(10), 10);
    }

    @Test
    void 만기일시_이자만_납부() {
        var s = LoanScheduleCalculator.calculate(3 * EOK, 0.04, 30, RepaymentMethod.BULLET);
        assertEquals(1_000_000, s.firstMonthPayment());
        assertEquals(3 * EOK, s.balanceAfterYear(10));
        assertEquals(360_000_000, s.totalInterest(), 10);
    }

    // ───────── 취득비용 ─────────

    @Test
    void 취득세_1주택_8억_6억9억_구간산식() {
        var r = AcquisitionCostCalculator.calculate(p, 8 * EOK, 84.9, 1, true, false, 0);
        // 8 × 2/3 − 3 = 2.3333% → 소수점 넷째자리 0.0233
        assertEquals(0.0233, r.acquisitionTaxRate(), 1e-9);
        assertEquals(18_640_000, r.acquisitionTax());
        assertEquals(1_864_000, r.localEducationTax());
        assertEquals(0, r.ruralSpecialTax()); // 85㎡ 이하
        // 중개보수 0.4% × 1.1(부가세)
        assertEquals(3_520_000, r.brokerageFee());
        assertEquals(1_600_000, r.miscCost());
        assertEquals(18_640_000 + 1_864_000 + 3_520_000 + 1_600_000, r.total());
    }

    @Test
    void 취득세_경계값() {
        assertEquals(0.01, AcquisitionCostCalculator.basicRate(p, 6 * EOK), 1e-9);
        assertEquals(0.03, AcquisitionCostCalculator.basicRate(p, 9 * EOK), 1e-9);
        assertEquals(0.03, AcquisitionCostCalculator.basicRate(p, 10 * EOK), 1e-9);
    }

    @Test
    void 취득세_조정지역_2주택_중과_와_농특세() {
        var r = AcquisitionCostCalculator.calculate(p, 8 * EOK, 100, 2, true, false, 0);
        assertTrue(r.heavyTaxed());
        assertEquals(64_000_000, r.acquisitionTax());
        assertEquals(3_200_000, r.localEducationTax());
        assertEquals(4_800_000, r.ruralSpecialTax()); // 0.6%
    }

    @Test
    void 생애최초_감면_200만원() {
        var r = AcquisitionCostCalculator.calculate(p, 5 * EOK, 59, 1, true, true, 0);
        assertEquals(5_000_000, r.acquisitionTaxBeforeRelief());
        assertEquals(2_000_000, r.firstTimeRelief());
        assertEquals(3_000_000, r.acquisitionTax());
    }

    // ───────── 대출 한도 ─────────

    private LoanLimitCalculator.Input limitInput(long price, BorrowerType b, UsageType u, long deposit) {
        return new LoanLimitCalculator.Input(price, b, u, deposit, 100_000_000L, 0, 0.04, 30,
                RepaymentMethod.EQUAL_PAYMENT, RateType.VARIABLE);
    }

    @Test
    void 규제지역_무주택_LTV40_이_한도() {
        var r = LoanLimitCalculator.calculate(p, TestPolicy.SEOUL, limitInput(10 * EOK, BorrowerType.NO_HOUSE, UsageType.OWNER_OCCUPY, 0));
        assertTrue(r.allowed());
        assertEquals(4 * EOK, r.ltvLimit());
        assertEquals(6 * EOK, (long) r.capLimit());
        assertEquals(0.07, r.dsrTestRate(), 1e-9);
        assertEquals(4 * EOK, r.maxLoan());
        assertEquals("LTV", r.bindingConstraint());
    }

    @Test
    void 규제지역_생애최초는_DSR이_한도() {
        var r = LoanLimitCalculator.calculate(p, TestPolicy.SEOUL, limitInput(10 * EOK, BorrowerType.FIRST_TIME, UsageType.OWNER_OCCUPY, 0));
        // 연소득 1억 × 40% = 4,000만원 / 스트레스 7%·30년 원리금균등 → 약 5.01억
        assertEquals("DSR", r.bindingConstraint());
        assertEquals(501_000_000, r.maxLoan(), 1_500_000);
    }

    @Test
    void 규제지역_20억은_4억_절대한도() {
        var in = new LoanLimitCalculator.Input(20 * EOK, BorrowerType.FIRST_TIME, UsageType.OWNER_OCCUPY, 0,
                500_000_000L, 0, 0.04, 30, RepaymentMethod.EQUAL_PAYMENT, RateType.VARIABLE);
        var r = LoanLimitCalculator.calculate(p, TestPolicy.SEOUL, in);
        assertEquals(4 * EOK, r.maxLoan());
        assertEquals("주택가격별 한도", r.bindingConstraint());
    }

    @Test
    void 수도권_전세끼고_매수는_주담대_불가() {
        var r = LoanLimitCalculator.calculate(p, TestPolicy.BUCHEON, limitInput(6 * EOK, BorrowerType.NO_HOUSE, UsageType.JEONSE, 4 * EOK));
        assertFalse(r.allowed());
        assertEquals(0, r.maxLoan());
    }

    @Test
    void 지방_임대는_보증금_차감() {
        var r = LoanLimitCalculator.calculate(p, TestPolicy.DAEJEON, limitInput(5 * EOK, BorrowerType.ONE_HOUSE, UsageType.MONTHLY_RENT, 50_000_000L));
        // 5억 × 60% − 보증금 5천 = 2.5억
        assertEquals(250_000_000, r.ltvLimit());
        assertNull(r.capLimit());
    }

    @Test
    void 혼합형은_스트레스_80퍼센트_반영() {
        var in = new LoanLimitCalculator.Input(10 * EOK, BorrowerType.NO_HOUSE, UsageType.OWNER_OCCUPY, 0,
                100_000_000L, 0, 0.04, 30, RepaymentMethod.EQUAL_PAYMENT, RateType.MIXED);
        var r = LoanLimitCalculator.calculate(p, TestPolicy.SEOUL, in);
        assertEquals(0.024, r.stressRateApplied(), 1e-9);
    }

    // ───────── 세금 ─────────

    @Test
    void 재산세_1주택_8억() {
        var t = TaxCalculators.holdingTax(p, 8 * EOK, 0.69, true);
        // 공시 5.52억 × 44% = 242,880,000 → 0.25% − 18만 = 427,200
        assertEquals(552_000_000, t.publicPrice());
        assertEquals(427_200, t.propertyTax());
        assertEquals(340_032, t.urbanAreaTax());
        assertEquals(85_440, t.localEducationTax());
        assertFalse(t.comprehensiveTaxPossible());
    }

    @Test
    void 양도세_1주택_12억이하_비과세() {
        var t = TaxCalculators.capitalGainsTax(p, new TaxCalculators.CapitalGainsInput(8 * EOK, 10 * EOK, 0, 0, 5, 5, true, true));
        assertTrue(t.fullyExempt());
        assertEquals(0, t.total());
    }

    @Test
    void 양도세_규제지역_거주2년미만이면_과세() {
        var t = TaxCalculators.capitalGainsTax(p, new TaxCalculators.CapitalGainsInput(8 * EOK, 10 * EOK, 0, 0, 5, 0, true, true));
        assertFalse(t.fullyExempt());
        assertTrue(t.total() > 0);
    }

    @Test
    void 양도세_1주택_고가주택_장특공80() {
        var t = TaxCalculators.capitalGainsTax(p, new TaxCalculators.CapitalGainsInput(8 * EOK, 15 * EOK, 0, 0, 10, 10, true, true));
        // 차익 7억 × (3/15) = 1.4억, 장특공 80% → 2,800만 − 250만 = 2,550만 → 15% − 126만 = 2,565,000
        assertEquals(0.8, t.longTermDeductionRate(), 1e-9);
        assertEquals(25_500_000, t.taxBase());
        assertEquals(2_565_000, t.incomeTax());
        assertEquals(2_821_500, t.total());
    }

    @Test
    void 양도세_일반_5년보유() {
        var t = TaxCalculators.capitalGainsTax(p, new TaxCalculators.CapitalGainsInput(5 * EOK, 6 * EOK, 0, 0, 5, 0, false, false));
        // 1억 − 장특공 10% − 250만 = 8,750만 → 24% − 576만 = 1,524만
        assertEquals(15_240_000, t.incomeTax());
    }

    @Test
    void 양도세_단기_1년이상_2년미만_60퍼센트() {
        var t = TaxCalculators.capitalGainsTax(p, new TaxCalculators.CapitalGainsInput(5 * EOK, 6 * EOK, 0, 0, 1, 0, false, false));
        assertEquals(58_500_000, t.incomeTax());
    }

    // ───────── IRR ─────────

    @Test
    void IRR_기본() {
        assertEquals(0.10, Irr.compute(new double[]{-100, 110}), 1e-6);
        assertEquals(0.10, Irr.compute(new double[]{-100, 0, 121}), 1e-6);
        assertNull(Irr.compute(new double[]{100, 100}));
    }

    // ───────── 통합 시뮬레이션 ─────────

    static InvestmentAnalyzer.Input sampleInput(UsageType usage, long deposit, long rent) {
        return new InvestmentAnalyzer.Input(
                300_000_000L, 8_000_000L, 0, 0, "테스트아파트",
                8 * EOK, 84.9, 0, false, false,
                usage, usage == UsageType.JEONSE ? deposit : 0, usage == UsageType.MONTHLY_RENT ? deposit : 0, rent, 0,
                null, 0.04, 30, RepaymentMethod.EQUAL_PAYMENT, RateType.VARIABLE,
                5, -0.01, 0.03, 0.06, null, 0, null);
    }

    @Test
    void 통합_실거주_순수익_항등식() {
        var r = InvestmentAnalyzer.analyze(p, TestPolicy.SEOUL, sampleInput(UsageType.OWNER_OCCUPY, 0, 0));
        var f = r.funding();
        // 부족분만 대출: 8억 + 취득비용 − 현금 3억, LTV 40%(3.2억) 이내
        assertEquals(320_000_000, f.maxLoan());
        assertTrue(f.loanAmount() <= f.maxLoan());
        assertEquals(f.price() + f.acquisitionCost() - f.loanAmount(), f.requiredCash());

        assertEquals(3, r.scenarios().size());
        for (var s : r.scenarios()) {
            assertEquals(10, s.years().size());
            var e = s.exit();
            assertEquals(5, e.holdingYears());
            long expected = e.salePrice() - f.price() - f.acquisitionCost() - e.sellingCost()
                    - e.capitalGainsTax().total() - e.totalInterest() - e.totalHoldingTax() - e.totalOwnerCost()
                    + e.totalRentIncome();
            assertEquals(expected, e.netProfit(), 10);
            long sumFlows = e.cashFlows().stream().mapToLong(Long::longValue).sum();
            assertEquals(sumFlows, e.netProfit(), 10);
        }
        // 기준 시나리오 5년 후 가격 = 8억 × 1.03^5
        assertEquals(Math.round(8 * EOK * Math.pow(1.03, 5)), r.scenarios().get(1).exit().salePrice());
        // 낙관 > 기준 > 보수
        assertTrue(r.scenarios().get(2).exit().netProfit() > r.scenarios().get(1).exit().netProfit());
        assertTrue(r.scenarios().get(1).exit().netProfit() > r.scenarios().get(0).exit().netProfit());
    }

    @Test
    void 통합_서울_전세끼고_매수는_대출0_경고() {
        var r = InvestmentAnalyzer.analyze(p, TestPolicy.SEOUL, sampleInput(UsageType.JEONSE, 5 * EOK, 0));
        assertEquals(0, r.funding().loanAmount());
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("토지거래허가구역")));
        // 초기 투자금 = 매매가 + 취득비용 − 보증금
        var e = r.scenarios().get(1).exit();
        assertEquals(8 * EOK + r.funding().acquisitionCost() - 5 * EOK, e.initialInvestment());
    }

    @Test
    void 입력검증() {
        var bad = sampleInput(UsageType.OWNER_OCCUPY, 0, 0);
        var in = new InvestmentAnalyzer.Input(bad.cash(), bad.monthlyIncome(), 0, 0, "x", 0, 84, 0, false, false,
                UsageType.OWNER_OCCUPY, 0, 0, 0, 0, null, 0.04, 30, RepaymentMethod.EQUAL_PAYMENT, RateType.VARIABLE,
                5, 0, 0, 0, null, 0, null);
        assertThrows(IllegalArgumentException.class, () -> InvestmentAnalyzer.analyze(p, TestPolicy.SEOUL, in));
    }
}
