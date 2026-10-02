package com.propintel.domain.market.calc;

import com.propintel.domain.market.calc.MarketStatsCalculator.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarketStatsCalculatorTest {

    static final long EOK = 100_000_000L;

    static SaleRow sale(String date, double priceEok) {
        return new SaleRow(1, "샘플단지", 84.0, 10, Math.round(priceEok * EOK), LocalDate.parse(date));
    }

    static List<SaleRow> sampleSales() {
        List<SaleRow> l = new ArrayList<>();
        for (String d : new String[]{"2025-08-03", "2025-08-12", "2025-08-20"}) l.add(sale(d, 8.0));
        for (String d : new String[]{"2026-03-03", "2026-03-12", "2026-03-20"}) l.add(sale(d, 8.0));
        for (String d : new String[]{"2026-05-03", "2026-05-12", "2026-05-20"}) l.add(sale(d, 8.0));
        for (String d : new String[]{"2026-06-03", "2026-06-12", "2026-06-20"}) l.add(sale(d, 8.4));
        for (String d : new String[]{"2026-08-05", "2026-08-10", "2026-08-15", "2026-08-25"}) l.add(sale(d, 8.8));
        l.add(sale("2026-09-10", 8.3)); // 직전 8.8억 대비 -5.7% → 하락 거래
        return l;
    }

    static List<RentRow> sampleRents() {
        return List.of(
                new RentRow(1, "샘플단지", 84.0, 5, Math.round(5.28 * EOK), 0, LocalDate.parse("2026-08-02"), "신규"),
                new RentRow(1, "샘플단지", 84.0, 7, Math.round(5.28 * EOK), 0, LocalDate.parse("2026-08-22"), "갱신"),
                new RentRow(1, "샘플단지", 84.0, 3, 50_000_000L, 2_000_000L, LocalDate.parse("2026-08-11"), "신규"));
    }

    Stats stats() {
        return MarketStatsCalculator.compute(sampleSales(), sampleRents(), Options.defaults(YearMonth.of(2026, 9), 3));
    }

    @Test
    void 기준월은_거래3건이상인_최근월() {
        Stats s = stats();
        assertEquals("2026-08", s.refMonth());
        assertEquals(4, s.refSampleSize());
        assertEquals(880_000_000L, s.currentPrice84());
    }

    @Test
    void 기간별_변동률() {
        Stats s = stats();
        assertNull(s.change(1).rate());                       // 2026-07 거래 없음
        assertEquals(0.10, s.change(3).rate(), 1e-9);         // 2026-05 8.0억 → 8.8억
        assertEquals(0.10, s.change(12).rate(), 1e-9);        // 2025-08 8.0억 → 8.8억
        assertNotNull(s.change(1).note());
    }

    @Test
    void 거래량_급증_판단() {
        Stats s = stats();
        // 직전 6개월(2026-02~07) 합계 9건 / 6 = 1.5건, 기준월 4건 → 2.67배
        assertEquals(1.5, s.volume().baselineAvg(), 1e-9);
        assertEquals(4 / 1.5, s.volume().ratio(), 1e-9);
        assertEquals("급증", s.volume().trend());
    }

    @Test
    void 신고가와_하락거래() {
        Stats s = stats();
        assertEquals(1, s.newHighs().size());                 // 2026-08-05 8.8억 > 이전 최고 8.4억 (최근 3개월 내)
        assertEquals(LocalDate.parse("2026-08-05"), s.newHighs().get(0).date());
        assertEquals(840_000_000L, s.newHighs().get(0).referencePrice());
        assertEquals(1, s.drops().size());
        assertEquals(-0.0568, s.drops().get(0).diffRate(), 1e-3);
    }

    @Test
    void 전세가율과_전세가() {
        Stats s = stats();
        assertEquals(528_000_000L, s.jeonse().price84());
        // 최근 3개월 매매 ㎡당 평균: (3×8.4 + 4×8.8)/7 억, 전세 5.28억
        double sale84 = (3 * 8.4 + 4 * 8.8) / 7.0;
        assertEquals(5.28 / sale84, s.jeonse().jeonseRatio(), 1e-6);
        assertEquals(1, s.monthly().stream().filter(m -> m.month().equals("2026-08")).findFirst().orElseThrow().monthlyRentCount());
    }

    @Test
    void 면적필터() {
        List<SaleRow> l = new ArrayList<>(sampleSales());
        l.add(new SaleRow(2, "큰평수", 135.0, 3, 20 * EOK, LocalDate.parse("2026-08-07")));
        Stats all = MarketStatsCalculator.compute(l, List.of(), Options.defaults(YearMonth.of(2026, 9), 3));
        Stats small = MarketStatsCalculator.compute(l, List.of(), Options.defaults(YearMonth.of(2026, 9), 3).withArea(59.0, 85.0));
        assertEquals(5, all.refSampleSize());
        assertEquals(4, small.refSampleSize());
        assertEquals(880_000_000L, small.currentPrice84());
    }

    @Test
    void 데이터가_없으면_기준월_없음() {
        Stats s = MarketStatsCalculator.compute(List.of(), List.of(), Options.defaults(YearMonth.of(2026, 9), 3));
        assertNull(s.refMonth());
        assertEquals(24, s.monthly().size());
    }

    // ───── 알림 오탐 방지 ─────

    /** 단지 A(8억)·B(12억)·C(10억)에서 매달 99건, 가격 변동 없음 (2025-10 ~ 2026-09) */
    static List<SaleRow> steadyMarket() {
        long[] prices = {8 * EOK, 12 * EOK, 10 * EOK};
        List<SaleRow> l = new ArrayList<>();
        for (YearMonth m = YearMonth.of(2025, 10); !m.isAfter(YearMonth.of(2026, 9)); m = m.plusMonths(1)) {
            for (int i = 0; i < 99; i++) {
                int k = i % 3;
                l.add(new SaleRow(k + 1, "단지" + (k + 1), 84.0, 10, prices[k], m.atDay(10)));
            }
        }
        return l;
    }

    @Test
    void 월초_집계중인_달은_기준월로_쓰지_않는다() {
        List<SaleRow> l = new ArrayList<>(steadyMarket());
        for (int i = 0; i < 4; i++) l.add(new SaleRow(1, "A", 84.0, 10, 8 * EOK, LocalDate.parse("2026-10-01")));
        Stats s = MarketStatsCalculator.compute(l, List.of(),
                Options.forRegion(LocalDate.parse("2026-10-02"), null, null, 24));
        // 10월(4건)·9월(신고기한 10/30까지)은 집계 중 → 8월이 기준월
        assertEquals("2026-08", s.refMonth());
        assertEquals("보합", s.volume().trend());
        assertEquals(0.0, s.change(1).rate(), 1e-9);
    }

    @Test
    void 신고기한이_지나면_그_달이_기준월이_된다() {
        Stats s = MarketStatsCalculator.compute(steadyMarket(), List.of(),
                Options.forRegion(LocalDate.parse("2026-10-30"), null, null, 24));
        assertEquals("2026-09", s.refMonth());
    }

    @Test
    void 거래_단지_구성만_바뀌면_변동률은_0() {
        List<SaleRow> l = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            l.add(new SaleRow(1, "A", 84.0, 10, 8 * EOK, LocalDate.parse("2026-07-10")));
            l.add(new SaleRow(2, "B", 84.0, 10, 12 * EOK, LocalDate.parse("2026-07-10")));
            l.add(new SaleRow(3, "C", 59.0, 10, 6 * EOK, LocalDate.parse("2026-07-10")));
        }
        for (int i = 0; i < 100; i++) { // 8월: 비싼 단지 B 거래가 80%
            boolean b = i < 80;
            l.add(new SaleRow(b ? 2 : 1, b ? "B" : "A", 84.0, 10, b ? 12 * EOK : 8 * EOK, LocalDate.parse("2026-08-10")));
        }
        l.add(new SaleRow(3, "C", 59.0, 10, 6 * EOK, LocalDate.parse("2026-08-10")));
        Stats s = MarketStatsCalculator.compute(l, List.of(), Options.forRegion(LocalDate.parse("2026-10-01"), null, null, 24));
        assertEquals("2026-08", s.refMonth());
        assertEquals(0.0, s.change(1).rate(), 1e-9);   // 예전 방식(월 전체 평균)이면 약 +10%
        assertEquals(3, s.change(1).pairedGroups());
    }

    @Test
    void 비교쌍이_부족하면_변동률을_비운다() {
        List<SaleRow> l = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            l.add(new SaleRow(1, "A", 84.0, 10, 8 * EOK, LocalDate.parse("2026-07-10")));
            l.add(new SaleRow(2, "B", 84.0, 10, 12 * EOK, LocalDate.parse("2026-08-10")));
        }
        Stats s = MarketStatsCalculator.compute(l, List.of(), Options.forRegion(LocalDate.parse("2026-10-01"), null, null, 24));
        assertNull(s.change(1).rate());
        assertEquals(0, s.change(1).pairedGroups());
        assertNotNull(s.change(1).note());
    }
}
