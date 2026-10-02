package com.propintel.domain.investment.calc;

import com.propintel.domain.investment.calc.policy.Bracket;
import com.propintel.domain.investment.calc.policy.PolicySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 취득 관련 비용: 취득세 + 지방교육세 + 농어촌특별세 + 중개보수 + 법무비 등 부대비용.
 * 모든 세율·기준금액은 PolicySnapshot 파라미터에서 읽는다.
 */
public final class AcquisitionCostCalculator {

    private AcquisitionCostCalculator() {}

    public record Result(
            long price,
            int housesAfterPurchase,
            boolean heavyTaxed,
            double acquisitionTaxRate,
            long acquisitionTaxBeforeRelief,
            long firstTimeRelief,
            long acquisitionTax,
            long localEducationTax,
            long ruralSpecialTax,
            long totalTax,
            double brokerageRate,
            long brokerageFee,
            long miscCost,
            long otherCost,
            long total,
            List<String> notes) {}

    /**
     * @param housesAfterPurchase 취득 후 주택 수 (일시적 2주택으로 기존주택 처분 예정이면 1로 계산)
     * @param regulated           취득 주택이 조정대상지역인지
     */
    public static Result calculate(PolicySnapshot p, long price, double exclusiveAreaM2,
                                   int housesAfterPurchase, boolean regulated, boolean firstTime,
                                   long otherCost) {
        List<String> notes = new ArrayList<>();
        int houses = Math.max(1, housesAfterPurchase);

        double rate;
        boolean heavy;
        if (houses == 1) {
            rate = basicRate(p, price);
            heavy = false;
        } else if (houses == 2) {
            heavy = regulated;
            rate = regulated ? p.param("acq.heavy.2house.regulated") : basicRate(p, price);
        } else if (houses == 3) {
            heavy = true;
            rate = regulated ? p.param("acq.heavy.3house.regulated") : p.param("acq.heavy.3house.nonregulated");
        } else {
            heavy = true;
            rate = p.param("acq.heavy.4house");
        }
        if (heavy) notes.add("다주택 취득세 중과세율(" + pct(rate) + ")이 적용되었습니다.");

        double taxBefore = price * rate;
        double relief = 0;
        if (firstTime && houses == 1 && price <= p.param("acq.firstTime.priceLimit")) {
            relief = Math.min(taxBefore, p.param("acq.firstTime.maxRelief"));
            notes.add("생애최초 주택 구입 취득세 감면(최대 " + won(p.param("acq.firstTime.maxRelief")) + ")을 가정했습니다. 감면 후 3개월 내 전입 등 요건이 있습니다.");
        }
        double acqTax = taxBefore - relief;

        double eduTax = heavy ? price * p.param("acq.edu.heavyRate")
                : price * rate * p.param("acq.edu.basicFactor");

        double rural = 0;
        if (exclusiveAreaM2 > p.param("acq.rural.areaThreshold")) {
            double ruralRate;
            if (!heavy) ruralRate = p.param("acq.rural.basic");
            else if (rate >= 0.12) ruralRate = p.param("acq.rural.heavy12");
            else ruralRate = p.param("acq.rural.heavy8");
            rural = price * ruralRate;
        } else {
            notes.add("전용면적 85㎡ 이하로 농어촌특별세는 비과세로 계산했습니다.");
        }

        BrokerageCalculator.Result brokerage = BrokerageCalculator.calculate(p, price);
        double misc = price * p.param("acq.miscCostRate");

        long totalTax = Math.round(acqTax) + Math.round(eduTax) + Math.round(rural);
        long total = totalTax + brokerage.fee() + Math.round(misc) + otherCost;

        return new Result(price, houses, heavy, rate, Math.round(taxBefore), Math.round(relief),
                Math.round(acqTax), Math.round(eduTax), Math.round(rural), totalTax,
                brokerage.rate(), brokerage.fee(), Math.round(misc), otherCost, total, notes);
    }

    /** 1주택 기본세율: 6억 이하 1%, 6~9억 구간 산식, 9억 초과 3% */
    static double basicRate(PolicySnapshot p, long price) {
        double low = p.param("acq.basic.lowThreshold");
        double high = p.param("acq.basic.highThreshold");
        if (price <= low) return p.param("acq.basic.lowRate");
        if (price > high) return p.param("acq.basic.highRate");
        // (취득가액 × 2/3억 − 3) × 1/100, 소수점 넷째자리까지
        double pctValue = price * 2.0 / 3.0 / 100_000_000.0 - 3.0;
        return Math.round(pctValue / 100.0 * 10_000.0) / 10_000.0;
    }

    static String pct(double r) { return String.format("%.2f%%", r * 100); }

    static String won(double v) {
        return String.format("%,.0f원", v);
    }

    /** 중개보수 (매매) */
    public static final class BrokerageCalculator {
        private BrokerageCalculator() {}

        public record Result(double rate, long fee, boolean capped) {}

        public static Result calculate(PolicySnapshot p, long price) {
            for (Bracket b : p.brackets("BROKERAGE_SALE")) {
                if (b.contains(price)) {
                    double fee = price * b.rate();
                    boolean capped = false;
                    if (b.cap() != null && fee > b.cap()) { fee = b.cap(); capped = true; }
                    fee *= (1 + p.param("brokerage.vatRate"));
                    return new Result(b.rate(), Math.round(fee), capped);
                }
            }
            throw new IllegalStateException("중개보수 요율 구간을 찾을 수 없습니다: " + price);
        }
    }
}
