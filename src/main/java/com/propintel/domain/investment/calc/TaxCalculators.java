package com.propintel.domain.investment.calc;

import com.propintel.domain.investment.calc.policy.Bracket;
import com.propintel.domain.investment.calc.policy.PolicySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 보유세(재산세 추정)와 양도소득세(추정) 계산.
 * 단순화 가정이 많으므로 결과에는 항상 notes 로 가정을 함께 돌려준다.
 */
public final class TaxCalculators {

    private TaxCalculators() {}

    // ───────────────────────── 보유세 ─────────────────────────

    public record HoldingTax(
            long marketPrice,
            long publicPrice,
            double fairMarketRatio,
            long taxBase,
            long propertyTax,
            long urbanAreaTax,
            long localEducationTax,
            long total,
            boolean comprehensiveTaxPossible,
            List<String> notes) {}

    /**
     * 재산세(+도시지역분, 지방교육세) 연간 추정액.
     * 종합부동산세는 계산하지 않고, 기준 초과 가능성만 표시한다.
     */
    public static HoldingTax holdingTax(PolicySnapshot p, long marketPrice, double publicPriceRatio, boolean oneHouse) {
        List<String> notes = new ArrayList<>();
        long publicPrice = Math.round(marketPrice * publicPriceRatio);
        double fmv;
        if (oneHouse) {
            if (publicPrice <= 300_000_000L) fmv = p.param("ptax.fmv.oneHouse.le3");
            else if (publicPrice <= 600_000_000L) fmv = p.param("ptax.fmv.oneHouse.le6");
            else fmv = p.param("ptax.fmv.oneHouse.over6");
        } else {
            fmv = p.param("ptax.fmv.standard");
        }
        double base = publicPrice * fmv;
        double propertyTax = progressive(p.brackets("PROPERTY_TAX"), base);
        double urban = base * p.param("ptax.urbanRate");
        double edu = propertyTax * p.param("ptax.eduRate");

        double threshold = oneHouse ? p.param("ptax.comprehensive.oneHouseThreshold")
                : p.param("ptax.comprehensive.threshold");
        boolean comp = publicPrice > threshold;
        if (comp) notes.add(String.format("추정 공시가격(%,d원)이 종합부동산세 공제기준(%,.0f원)을 넘을 수 있어 종부세가 추가될 수 있습니다(미계산).",
                publicPrice, threshold));
        notes.add("공시가격은 시세 × 공시가격 비율(" + Math.round(publicPriceRatio * 100) + "%)로 추정했으며, 세부담상한은 반영하지 않았습니다.");

        long total = Math.round(propertyTax) + Math.round(urban) + Math.round(edu);
        return new HoldingTax(marketPrice, publicPrice, fmv, Math.round(base), Math.round(propertyTax),
                Math.round(urban), Math.round(edu), total, comp, notes);
    }

    // ───────────────────────── 양도소득세 ─────────────────────────

    public record CapitalGainsInput(
            long purchasePrice,
            long salePrice,
            long acquisitionExpenses,
            long sellingExpenses,
            int holdingYears,
            int residenceYears,
            boolean oneHouseAtSale,
            boolean regulatedAtAcquisition) {}

    public record CapitalGainsTax(
            long gain,
            boolean fullyExempt,
            boolean oneHouseExemptionApplied,
            double taxableRatio,
            long taxableGainBeforeDeduction,
            double longTermDeductionRate,
            long longTermDeduction,
            long basicDeduction,
            long taxBase,
            String rateDescription,
            long incomeTax,
            long localIncomeTax,
            long total,
            List<String> notes) {}

    public static CapitalGainsTax capitalGainsTax(PolicySnapshot p, CapitalGainsInput in) {
        List<String> notes = new ArrayList<>();
        long gain = in.salePrice() - in.purchasePrice() - in.acquisitionExpenses() - in.sellingExpenses();
        if (gain <= 0) {
            notes.add("양도차익이 없어(손실) 양도소득세가 발생하지 않는 것으로 계산했습니다.");
            return new CapitalGainsTax(gain, true, false, 0, 0, 0, 0, 0, 0, "과세 없음", 0, 0, 0, notes);
        }

        int hold = in.holdingYears();
        boolean oneHouseEligible = in.oneHouseAtSale()
                && hold >= p.param("cgt.oneHouse.minHoldYears")
                && (!in.regulatedAtAcquisition() || in.residenceYears() >= p.param("cgt.oneHouse.minResidenceYearsRegulated"));

        double taxableRatio = 1.0;
        double ltdRate;
        if (oneHouseEligible) {
            double exemptPrice = p.param("cgt.oneHouse.exemptPrice");
            if (in.salePrice() <= exemptPrice) {
                notes.add("1세대 1주택 비과세 요건(보유 " + hold + "년"
                        + (in.regulatedAtAcquisition() ? ", 거주 " + in.residenceYears() + "년" : "")
                        + ")을 충족하고 양도가액이 비과세 기준 이하라고 가정했습니다.");
                return new CapitalGainsTax(gain, true, true, 0, 0, 0, 0, 0, 0, "1세대1주택 비과세", 0, 0, 0, notes);
            }
            taxableRatio = (in.salePrice() - exemptPrice) / in.salePrice();
            notes.add(String.format("1세대 1주택 고가주택: 양도차익 중 %.1f%%만 과세 대상으로 계산했습니다.", taxableRatio * 100));
            ltdRate = oneHouseLongTermRate(p, hold, in.residenceYears());
        } else {
            ltdRate = generalLongTermRate(p, hold);
            if (in.oneHouseAtSale() && hold < p.param("cgt.oneHouse.minHoldYears")) {
                notes.add("보유기간이 2년 미만이라 1세대 1주택 비과세가 적용되지 않습니다.");
            } else if (in.oneHouseAtSale()) {
                notes.add("조정대상지역 취득 주택은 2년 이상 거주해야 1세대 1주택 비과세가 적용됩니다.");
            }
            if (!in.oneHouseAtSale()) {
                notes.add("다주택 상태로 양도한다고 보고 일반세율을 적용했습니다. 다주택자 양도세 중과(유예 여부 포함)는 반영하지 않았으니 반드시 확인하세요.");
            }
        }

        double taxableGain = gain * taxableRatio;
        double ltd = taxableGain * ltdRate;
        double basic = p.param("cgt.basicDeduction");
        double base = Math.max(0, taxableGain - ltd - basic);

        double incomeTax;
        String rateDesc;
        if (hold < 1) {
            incomeTax = base * p.param("cgt.shortTerm.lt1");
            rateDesc = "단기양도 " + Math.round(p.param("cgt.shortTerm.lt1") * 100) + "% (1년 미만)";
        } else if (hold < 2) {
            incomeTax = base * p.param("cgt.shortTerm.lt2");
            rateDesc = "단기양도 " + Math.round(p.param("cgt.shortTerm.lt2") * 100) + "% (2년 미만)";
        } else {
            incomeTax = progressive(p.brackets("CGT"), base);
            rateDesc = "기본세율(6~45% 누진)";
        }
        double local = incomeTax * p.param("cgt.localTaxRate");
        long total = Math.round(incomeTax) + Math.round(local);

        return new CapitalGainsTax(gain, false, oneHouseEligible, taxableRatio, Math.round(taxableGain), ltdRate,
                Math.round(ltd), Math.round(Math.min(basic, Math.max(0, taxableGain - ltd))),
                Math.round(base), rateDesc, Math.round(incomeTax), Math.round(local), total, notes);
    }

    static double generalLongTermRate(PolicySnapshot p, int hold) {
        if (hold < p.param("cgt.ltd.minYears")) return 0;
        return Math.min(hold * p.param("cgt.ltd.general.perYear"), p.param("cgt.ltd.general.max"));
    }

    static double oneHouseLongTermRate(PolicySnapshot p, int hold, int reside) {
        if (hold < p.param("cgt.ltd.minYears")) return 0;
        if (reside < p.param("cgt.ltd.oneHouse.minResidence")) return generalLongTermRate(p, hold);
        double h = Math.min(hold * p.param("cgt.ltd.oneHouse.holdPerYear"), p.param("cgt.ltd.oneHouse.holdMax"));
        double r = Math.min(reside * p.param("cgt.ltd.oneHouse.residePerYear"), p.param("cgt.ltd.oneHouse.resideMax"));
        return h + r;
    }

    /** 누진세율 계산: 해당 구간 세율 × 과세표준 − 누진공제 */
    public static double progressive(List<Bracket> brackets, double base) {
        if (base <= 0) return 0;
        for (Bracket b : brackets) {
            if (b.contains(base)) return Math.max(0, base * b.rate() - b.deduction());
        }
        Bracket last = brackets.get(brackets.size() - 1);
        return Math.max(0, base * last.rate() - last.deduction());
    }
}
