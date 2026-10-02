package com.propintel.domain.investment.calc;

import com.propintel.domain.investment.calc.model.BorrowerType;
import com.propintel.domain.investment.calc.model.RegionInfo;
import com.propintel.domain.investment.calc.model.RegionType;
import com.propintel.domain.investment.calc.model.SourceMeta;
import com.propintel.domain.investment.calc.policy.Bracket;
import com.propintel.domain.investment.calc.policy.DsrRule;
import com.propintel.domain.investment.calc.policy.LoanCapTier;
import com.propintel.domain.investment.calc.policy.LoanRule;
import com.propintel.domain.investment.calc.policy.PolicyParam;
import com.propintel.domain.investment.calc.policy.PolicySnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.propintel.domain.investment.calc.model.BorrowerType.*;
import static com.propintel.domain.investment.calc.model.RegionType.*;

/** 테스트용 정책 스냅샷 — V2__seed_policy.sql 과 같은 값 */
final class TestPolicy {

    static final SourceMeta SRC = new SourceMeta("테스트", null, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 28), false, null);
    static final long EOK = 100_000_000L;

    static final RegionInfo SEOUL = new RegionInfo("11560", "서울특별시", "영등포구", true, true, true, SRC);
    static final RegionInfo BUCHEON = new RegionInfo("41190", "경기도", "부천시", true, false, false, SRC);
    static final RegionInfo DAEJEON = new RegionInfo("30200", "대전광역시", "유성구", false, false, false, SRC);

    static PolicySnapshot snapshot() {
        List<LoanRule> rules = new ArrayList<>();
        rule(rules, REGULATED, FIRST_TIME, true, 0.70, true);
        rule(rules, REGULATED, NO_HOUSE, true, 0.40, true);
        rule(rules, REGULATED, ONE_HOUSE_DISPOSING, true, 0.40, true);
        rule(rules, REGULATED, ONE_HOUSE, false, 0, true);
        rule(rules, REGULATED, MULTI_HOUSE, false, 0, true);
        rule(rules, METRO_NON_REGULATED, FIRST_TIME, true, 0.70, true);
        rule(rules, METRO_NON_REGULATED, NO_HOUSE, true, 0.70, true);
        rule(rules, METRO_NON_REGULATED, ONE_HOUSE_DISPOSING, true, 0.70, true);
        rule(rules, METRO_NON_REGULATED, ONE_HOUSE, false, 0, true);
        rule(rules, METRO_NON_REGULATED, MULTI_HOUSE, false, 0, true);
        rule(rules, NON_METRO, FIRST_TIME, true, 0.80, false);
        rule(rules, NON_METRO, NO_HOUSE, true, 0.70, false);
        rule(rules, NON_METRO, ONE_HOUSE_DISPOSING, true, 0.70, false);
        rule(rules, NON_METRO, ONE_HOUSE, true, 0.60, false);
        rule(rules, NON_METRO, MULTI_HOUSE, true, 0.60, false);

        List<LoanCapTier> caps = List.of(
                new LoanCapTier(REGULATED, 0, 15 * EOK, 6 * EOK, SRC),
                new LoanCapTier(REGULATED, 15 * EOK, 25 * EOK, 4 * EOK, SRC),
                new LoanCapTier(REGULATED, 25 * EOK, null, 2 * EOK, SRC),
                new LoanCapTier(METRO_NON_REGULATED, 0, null, 6 * EOK, SRC));

        List<DsrRule> dsr = List.of(
                new DsrRule(REGULATED, 0.40, 0.03, 1.0, 0.8, 0.4, SRC),
                new DsrRule(METRO_NON_REGULATED, 0.40, 0.03, 1.0, 0.8, 0.4, SRC),
                new DsrRule(NON_METRO, 0.40, 0.015, 1.0, 0.8, 0.4, SRC));

        List<Bracket> br = new ArrayList<>();
        br.add(new Bracket("BROKERAGE_SALE", 0, 50_000_000L, 0.006, 250_000L, 0, SRC));
        br.add(new Bracket("BROKERAGE_SALE", 50_000_000L, 2 * EOK, 0.005, 800_000L, 0, SRC));
        br.add(new Bracket("BROKERAGE_SALE", 2 * EOK, 9 * EOK, 0.004, null, 0, SRC));
        br.add(new Bracket("BROKERAGE_SALE", 9 * EOK, 12 * EOK, 0.005, null, 0, SRC));
        br.add(new Bracket("BROKERAGE_SALE", 12 * EOK, 15 * EOK, 0.006, null, 0, SRC));
        br.add(new Bracket("BROKERAGE_SALE", 15 * EOK, null, 0.007, null, 0, SRC));

        br.add(new Bracket("CGT", 0, 14_000_000L, 0.06, null, 0, SRC));
        br.add(new Bracket("CGT", 14_000_000L, 50_000_000L, 0.15, null, 1_260_000L, SRC));
        br.add(new Bracket("CGT", 50_000_000L, 88_000_000L, 0.24, null, 5_760_000L, SRC));
        br.add(new Bracket("CGT", 88_000_000L, 150_000_000L, 0.35, null, 15_440_000L, SRC));
        br.add(new Bracket("CGT", 150_000_000L, 300_000_000L, 0.38, null, 19_940_000L, SRC));
        br.add(new Bracket("CGT", 300_000_000L, 500_000_000L, 0.40, null, 25_940_000L, SRC));
        br.add(new Bracket("CGT", 500_000_000L, 1_000_000_000L, 0.42, null, 35_940_000L, SRC));
        br.add(new Bracket("CGT", 1_000_000_000L, null, 0.45, null, 65_940_000L, SRC));

        br.add(new Bracket("PROPERTY_TAX", 0, 60_000_000L, 0.001, null, 0, SRC));
        br.add(new Bracket("PROPERTY_TAX", 60_000_000L, 150_000_000L, 0.0015, null, 30_000L, SRC));
        br.add(new Bracket("PROPERTY_TAX", 150_000_000L, 300_000_000L, 0.0025, null, 180_000L, SRC));
        br.add(new Bracket("PROPERTY_TAX", 300_000_000L, null, 0.004, null, 630_000L, SRC));

        List<PolicyParam> ps = new ArrayList<>();
        p(ps, "acq.basic.lowThreshold", 600_000_000);
        p(ps, "acq.basic.highThreshold", 900_000_000);
        p(ps, "acq.basic.lowRate", 0.01);
        p(ps, "acq.basic.highRate", 0.03);
        p(ps, "acq.heavy.2house.regulated", 0.08);
        p(ps, "acq.heavy.3house.regulated", 0.12);
        p(ps, "acq.heavy.3house.nonregulated", 0.08);
        p(ps, "acq.heavy.4house", 0.12);
        p(ps, "acq.edu.basicFactor", 0.1);
        p(ps, "acq.edu.heavyRate", 0.004);
        p(ps, "acq.rural.areaThreshold", 85);
        p(ps, "acq.rural.basic", 0.002);
        p(ps, "acq.rural.heavy8", 0.006);
        p(ps, "acq.rural.heavy12", 0.01);
        p(ps, "acq.firstTime.maxRelief", 2_000_000);
        p(ps, "acq.firstTime.priceLimit", 1_200_000_000);
        p(ps, "acq.miscCostRate", 0.002);
        p(ps, "brokerage.vatRate", 0.1);
        p(ps, "ptax.publicPriceRatio.default", 0.69);
        p(ps, "ptax.fmv.standard", 0.60);
        p(ps, "ptax.fmv.oneHouse.le3", 0.43);
        p(ps, "ptax.fmv.oneHouse.le6", 0.44);
        p(ps, "ptax.fmv.oneHouse.over6", 0.45);
        p(ps, "ptax.urbanRate", 0.0014);
        p(ps, "ptax.eduRate", 0.20);
        p(ps, "ptax.comprehensive.oneHouseThreshold", 1_200_000_000);
        p(ps, "ptax.comprehensive.threshold", 900_000_000);
        p(ps, "cgt.oneHouse.exemptPrice", 1_200_000_000);
        p(ps, "cgt.oneHouse.minHoldYears", 2);
        p(ps, "cgt.oneHouse.minResidenceYearsRegulated", 2);
        p(ps, "cgt.basicDeduction", 2_500_000);
        p(ps, "cgt.shortTerm.lt1", 0.70);
        p(ps, "cgt.shortTerm.lt2", 0.60);
        p(ps, "cgt.localTaxRate", 0.10);
        p(ps, "cgt.ltd.minYears", 3);
        p(ps, "cgt.ltd.general.perYear", 0.02);
        p(ps, "cgt.ltd.general.max", 0.30);
        p(ps, "cgt.ltd.oneHouse.minResidence", 2);
        p(ps, "cgt.ltd.oneHouse.holdPerYear", 0.04);
        p(ps, "cgt.ltd.oneHouse.holdMax", 0.40);
        p(ps, "cgt.ltd.oneHouse.residePerYear", 0.04);
        p(ps, "cgt.ltd.oneHouse.resideMax", 0.40);

        return new PolicySnapshot(LocalDate.of(2026, 9, 28), rules, caps, dsr, br, ps);
    }

    private static void rule(List<LoanRule> l, RegionType r, BorrowerType b, boolean allowed, double ltv, boolean moveIn) {
        l.add(new LoanRule(r, b, allowed, ltv, moveIn, null, SRC));
    }

    private static void p(List<PolicyParam> l, String k, double v) {
        l.add(new PolicyParam(k, v, k, SRC));
    }
}
