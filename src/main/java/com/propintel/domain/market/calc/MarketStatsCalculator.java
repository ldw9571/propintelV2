package com.propintel.domain.market.calc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 실거래 목록으로 시장 지표를 계산하는 순수 함수 (DB·Spring 의존 없음 → 단위 테스트 가능).
 *
 * 면적이 다른 거래를 비교하기 위해 가격은 모두 "㎡당 가격 × 84" (84㎡ 환산가)로 평균한다.
 * 최근 월은 신고 지연(계약 후 30일)으로 거래가 덜 채워질 수 있어,
 * 거래가 minSample 건 이상인 가장 최근 월을 기준월로 삼는다.
 */
public final class MarketStatsCalculator {

    public static final double STANDARD_AREA = 84.0;

    private MarketStatsCalculator() {}

    // ───────────── 입력 ─────────────

    public record SaleRow(long complexId, String complexName, double area, int floor, long price, LocalDate date) {}

    public record RentRow(long complexId, String complexName, double area, int floor, long deposit,
                          long monthlyRent, LocalDate date, String contractType) {
        public boolean jeonse() { return monthlyRent == 0; }
    }

    public record Options(Double areaMin, Double areaMax, int months, int minSample, YearMonth today,
                          int notableMonths, double dropThreshold, int volumeBaselineMonths) {
        public static Options defaults(YearMonth today, int minSample) {
            return new Options(null, null, 24, minSample, today, 3, 0.05, 6);
        }

        public Options withArea(Double min, Double max) {
            return new Options(min, max, months, minSample, today, notableMonths, dropThreshold, volumeBaselineMonths);
        }

        boolean inArea(double a) {
            return (areaMin == null || a >= areaMin) && (areaMax == null || a <= areaMax);
        }
    }

    // ───────────── 출력 ─────────────

    public record MonthPoint(String month, int saleCount, Long saleAvgPrice, Long saleMedianPrice, Long salePrice84,
                             int jeonseCount, Long jeonseAvgDeposit, Long jeonsePrice84, int monthlyRentCount,
                             int newHighCount, int dropCount) {}

    public record Change(int months, Double rate, String baseMonth, Long basePrice84, int baseSample, String note) {}

    public record Volume(int refCount, Double baselineAvg, Double ratio, String trend, int baselineMonths) {}

    public record Jeonse(Long avgDeposit, Long price84, Double change1m, Double change3m, Double jeonseRatio,
                         int windowSaleCount, int windowJeonseCount, String note) {}

    public record NotableTrade(String kind, long complexId, String complexName, double area, int floor, long price,
                               LocalDate date, Long referencePrice, LocalDate referenceDate, double diffRate) {}

    public record Stats(String refMonth, Long currentAvgPrice, Long currentPrice84, int refSampleSize,
                        List<Change> changes, Volume volume, Jeonse jeonse,
                        List<NotableTrade> newHighs, List<NotableTrade> drops, List<MonthPoint> monthly,
                        LocalDate lastDealDate, int totalSales, int totalRents, List<String> notes) {

        public Change change(int months) {
            return changes.stream().filter(c -> c.months() == months).findFirst().orElse(null);
        }
    }

    // ───────────── 계산 ─────────────

    public static Stats compute(List<SaleRow> allSales, List<RentRow> allRents, Options o) {
        List<SaleRow> sales = allSales.stream().filter(s -> o.inArea(s.area()) && s.area() > 0 && s.price() > 0)
                .sorted(Comparator.comparing(SaleRow::date)).toList();
        List<RentRow> rents = allRents.stream().filter(r -> o.inArea(r.area()) && r.area() > 0).toList();
        List<String> notes = new ArrayList<>();

        // 1) 신고가 / 하락 거래 (전체 이력 기준으로 판단)
        List<NotableTrade> highsAll = new ArrayList<>();
        List<NotableTrade> dropsAll = new ArrayList<>();
        Map<Long, List<SaleRow>> byComplex = sales.stream().collect(Collectors.groupingBy(SaleRow::complexId));
        for (List<SaleRow> list : byComplex.values()) {
            for (int i = 0; i < list.size(); i++) {
                SaleRow s = list.get(i);
                SaleRow maxPrior = null, lastPrior = null;
                for (int j = 0; j < i; j++) {
                    SaleRow p = list.get(j);
                    if (!p.date().isBefore(s.date()) || Math.abs(p.area() - s.area()) > 2.0) continue;
                    if (maxPrior == null || p.price() > maxPrior.price()) maxPrior = p;
                    if (lastPrior == null || !p.date().isBefore(lastPrior.date())) lastPrior = p;
                }
                if (maxPrior != null && s.price() > maxPrior.price()) {
                    highsAll.add(new NotableTrade("NEW_HIGH", s.complexId(), s.complexName(), s.area(), s.floor(),
                            s.price(), s.date(), maxPrior.price(), maxPrior.date(),
                            (double) s.price() / maxPrior.price() - 1));
                }
                if (lastPrior != null && s.price() <= lastPrior.price() * (1 - o.dropThreshold())) {
                    dropsAll.add(new NotableTrade("DROP", s.complexId(), s.complexName(), s.area(), s.floor(),
                            s.price(), s.date(), lastPrior.price(), lastPrior.date(),
                            (double) s.price() / lastPrior.price() - 1));
                }
            }
        }

        // 2) 월별 집계
        YearMonth end = o.today();
        YearMonth start = end.minusMonths(o.months() - 1L);
        Map<YearMonth, List<SaleRow>> saleByMonth = sales.stream().collect(Collectors.groupingBy(s -> YearMonth.from(s.date())));
        Map<YearMonth, List<RentRow>> rentByMonth = rents.stream().collect(Collectors.groupingBy(r -> YearMonth.from(r.date())));
        Map<YearMonth, Long> highCount = highsAll.stream().collect(Collectors.groupingBy(t -> YearMonth.from(t.date()), Collectors.counting()));
        Map<YearMonth, Long> dropCount = dropsAll.stream().collect(Collectors.groupingBy(t -> YearMonth.from(t.date()), Collectors.counting()));

        List<MonthPoint> monthly = new ArrayList<>();
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            List<SaleRow> ms = saleByMonth.getOrDefault(m, List.of());
            List<RentRow> mr = rentByMonth.getOrDefault(m, List.of());
            List<RentRow> mj = mr.stream().filter(RentRow::jeonse).toList();
            monthly.add(new MonthPoint(m.toString(), ms.size(),
                    ms.isEmpty() ? null : Math.round(ms.stream().mapToLong(SaleRow::price).average().orElse(0)),
                    ms.isEmpty() ? null : median(ms.stream().map(SaleRow::price).toList()),
                    salePrice84(ms), mj.size(),
                    mj.isEmpty() ? null : Math.round(mj.stream().mapToLong(RentRow::deposit).average().orElse(0)),
                    jeonsePrice84(mj), mr.size() - mj.size(),
                    highCount.getOrDefault(m, 0L).intValue(), dropCount.getOrDefault(m, 0L).intValue()));
        }

        // 3) 기준월
        YearMonth ref = null;
        for (YearMonth m = end; !m.isBefore(start); m = m.minusMonths(1)) {
            if (saleByMonth.getOrDefault(m, List.of()).size() >= o.minSample()) { ref = m; break; }
        }
        if (ref == null) {
            ref = saleByMonth.keySet().stream().filter(m -> !m.isAfter(end)).max(Comparator.naturalOrder()).orElse(null);
            if (ref != null) notes.add("거래가 " + o.minSample() + "건 이상인 달이 없어 거래가 있는 가장 최근 달을 기준으로 했습니다(표본이 작아 변동이 큽니다).");
        }
        if (ref != null && ref.isBefore(end)) {
            notes.add("기준월은 " + ref + "입니다. 그 이후 달은 거래가 적거나 신고 기한(계약 후 30일)이 지나지 않아 덜 채워졌을 수 있습니다.");
        }

        Long currentAvg = null, current84 = null;
        int refSample = 0;
        List<Change> changes = new ArrayList<>();
        Volume volume = new Volume(0, null, null, "데이터 없음", o.volumeBaselineMonths());
        Jeonse jeonse = new Jeonse(null, null, null, null, null, 0, 0, "데이터 없음");

        if (ref != null) {
            List<SaleRow> rs = saleByMonth.getOrDefault(ref, List.of());
            refSample = rs.size();
            currentAvg = Math.round(rs.stream().mapToLong(SaleRow::price).average().orElse(0));
            current84 = salePrice84(rs);

            for (int n : new int[]{1, 3, 6, 12}) {
                YearMonth base = ref.minusMonths(n);
                List<SaleRow> bs = saleByMonth.getOrDefault(base, List.of());
                Long b84 = salePrice84(bs);
                if (bs.size() < o.minSample() || b84 == null || current84 == null) {
                    changes.add(new Change(n, null, base.toString(), b84, bs.size(),
                            "비교 월(" + base + ") 거래가 " + bs.size() + "건으로 부족"));
                } else {
                    changes.add(new Change(n, (double) current84 / b84 - 1, base.toString(), b84, bs.size(), null));
                }
            }

            // 거래량: 기준월 vs 직전 N개월 평균
            double sum = 0;
            for (int i = 1; i <= o.volumeBaselineMonths(); i++) sum += saleByMonth.getOrDefault(ref.minusMonths(i), List.of()).size();
            double avg = sum / o.volumeBaselineMonths();
            Double ratio = avg > 0 ? refSample / avg : null;
            volume = new Volume(refSample, avg, ratio, trend(ratio), o.volumeBaselineMonths());

            // 전세
            List<RentRow> rj = rentByMonth.getOrDefault(ref, List.of()).stream().filter(RentRow::jeonse).toList();
            Long j84 = jeonsePrice84(rj);
            Double jc1 = rate(j84, jeonsePrice84(jeonseOf(rentByMonth, ref.minusMonths(1))));
            Double jc3 = rate(j84, jeonsePrice84(jeonseOf(rentByMonth, ref.minusMonths(3))));
            List<SaleRow> ws = new ArrayList<>();
            List<RentRow> wj = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                ws.addAll(saleByMonth.getOrDefault(ref.minusMonths(i), List.of()));
                wj.addAll(jeonseOf(rentByMonth, ref.minusMonths(i)));
            }
            Long ws84 = salePrice84(ws), wj84 = jeonsePrice84(wj);
            Double jr = (ws84 != null && wj84 != null && ws84 > 0) ? (double) wj84 / ws84 : null;
            jeonse = new Jeonse(rj.isEmpty() ? null : Math.round(rj.stream().mapToLong(RentRow::deposit).average().orElse(0)),
                    j84, jc1, jc3, jr, ws.size(), wj.size(),
                    jr == null ? "전세 또는 매매 거래가 없어 전세가율을 계산하지 못했습니다." : "최근 3개월(" + ref.minusMonths(2) + "~" + ref + ") ㎡당 평균으로 계산");
        }

        LocalDate recentFrom = o.today().minusMonths(o.notableMonths() - 1L).atDay(1);
        List<NotableTrade> highs = highsAll.stream().filter(t -> !t.date().isBefore(recentFrom))
                .sorted(Comparator.comparing(NotableTrade::date).reversed()).toList();
        List<NotableTrade> drops = dropsAll.stream().filter(t -> !t.date().isBefore(recentFrom))
                .sorted(Comparator.comparing(NotableTrade::date).reversed()).toList();

        LocalDate last = sales.isEmpty() ? null : sales.get(sales.size() - 1).date();
        notes.add("해제(취소)된 거래는 수집 단계에서 제외했습니다. 가격은 전용 84㎡ 기준으로 환산한 ㎡당 평균입니다.");

        return new Stats(ref == null ? null : ref.toString(), currentAvg, current84, refSample, changes, volume, jeonse,
                highs, drops, monthly, last, sales.size(), rents.size(), notes);
    }

    static String trend(Double ratio) {
        if (ratio == null) return "비교 불가";
        if (ratio >= 2.0) return "급증";
        if (ratio >= 1.3) return "증가";
        if (ratio <= 0.5) return "급감";
        if (ratio <= 0.77) return "감소";
        return "보합";
    }

    private static List<RentRow> jeonseOf(Map<YearMonth, List<RentRow>> m, YearMonth ym) {
        return m.getOrDefault(ym, List.of()).stream().filter(RentRow::jeonse).toList();
    }

    private static Double rate(Long now, Long base) {
        if (now == null || base == null || base == 0) return null;
        return (double) now / base - 1;
    }

    static Long salePrice84(List<SaleRow> rows) {
        if (rows.isEmpty()) return null;
        return Math.round(rows.stream().mapToDouble(s -> s.price() / s.area()).average().orElse(0) * STANDARD_AREA);
    }

    static Long jeonsePrice84(List<RentRow> rows) {
        if (rows.isEmpty()) return null;
        return Math.round(rows.stream().mapToDouble(r -> r.deposit() / r.area()).average().orElse(0) * STANDARD_AREA);
    }

    static Long median(List<Long> values) {
        List<Long> v = new ArrayList<>(values);
        Collections.sort(v);
        int n = v.size();
        if (n == 0) return null;
        return n % 2 == 1 ? v.get(n / 2) : Math.round((v.get(n / 2 - 1) + v.get(n / 2)) / 2.0);
    }
}
