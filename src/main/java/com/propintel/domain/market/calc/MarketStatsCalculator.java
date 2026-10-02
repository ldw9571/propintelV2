package com.propintel.domain.market.calc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 실거래 목록으로 시장 지표를 계산하는 순수 함수 (DB·Spring 의존 없음 → 단위 테스트 가능).
 *
 * 면적이 다른 거래를 비교하기 위해 가격 수준은 "㎡당 가격 × 84" (84㎡ 환산가)로 평균한다.
 *
 * 기준월: 실거래는 계약 후 30일 안에 신고하므로, 월말 + 신고기한(reportLagDays)이 지난 "집계 완료" 달 중
 *         거래가 minSample 건 이상인 가장 최근 달. (집계 중인 달을 쓰면 월초마다 거래량 급감·가격 급변 오탐이 난다)
 * 변동률: 같은 단지·같은 평형끼리 ㎡당 가격 변동률을 구해 그 중앙값을 쓴다.
 *         (월별 전체 평균을 비교하면 비싼 단지 거래가 몰린 달에 가격이 오른 것처럼 보이는 구성 착시가 생긴다)
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

    /**
     * @param asOf          계산 기준일 (집계 완료 여부 판단)
     * @param reportLagDays 실거래 신고기한(일). 월말 + 이 기간이 지나야 그 달을 집계 완료로 본다
     * @param minPairs      변동률 계산에 필요한 최소 "같은 단지·평형" 비교쌍 수
     */
    public record Options(Double areaMin, Double areaMax, int months, int minSample, YearMonth today,
                          int notableMonths, double dropThreshold, int volumeBaselineMonths,
                          LocalDate asOf, int reportLagDays, int minPairs) {

        public static final int DEFAULT_REPORT_LAG_DAYS = 30;

        /** 테스트·기본값: 기준일은 today 달의 말일, 비교쌍 1개 이상 */
        public static Options defaults(YearMonth today, int minSample) {
            return new Options(null, null, 24, minSample, today, 3, 0.05, 6,
                    today.atEndOfMonth(), DEFAULT_REPORT_LAG_DAYS, 1);
        }

        /** 지역: 기준월 거래 3건 이상, 같은 단지·평형 비교쌍 3개 이상 */
        public static Options forRegion(LocalDate asOf, Double areaMin, Double areaMax, int months) {
            return new Options(areaMin, areaMax, months, 3, YearMonth.from(asOf), 3, 0.05, 6,
                    asOf, DEFAULT_REPORT_LAG_DAYS, 3);
        }

        /** 단지: 기준월 거래 1건 이상, 비교쌍 1개 이상 */
        public static Options forComplex(LocalDate asOf, Double areaMin, Double areaMax, int months) {
            return new Options(areaMin, areaMax, months, 1, YearMonth.from(asOf), 3, 0.05, 6,
                    asOf, DEFAULT_REPORT_LAG_DAYS, 1);
        }

        public Options withArea(Double min, Double max) {
            return new Options(min, max, months, minSample, today, notableMonths, dropThreshold, volumeBaselineMonths,
                    asOf, reportLagDays, minPairs);
        }

        boolean inArea(double a) {
            return (areaMin == null || a >= areaMin) && (areaMax == null || a <= areaMax);
        }

        /** 월말 + 신고기한이 지나 거래가 다 채워졌다고 볼 수 있는 달인지 */
        public boolean isComplete(YearMonth m) {
            return !m.atEndOfMonth().plusDays(reportLagDays).isAfter(asOf);
        }
    }

    // ───────────── 출력 ─────────────

    public record MonthPoint(String month, int saleCount, Long saleAvgPrice, Long saleMedianPrice, Long salePrice84,
                             int jeonseCount, Long jeonseAvgDeposit, Long jeonsePrice84, int monthlyRentCount,
                             int newHighCount, int dropCount) {}

    /**
     * @param rate         같은 단지·평형 비교쌍별 ㎡당 가격 변동률의 중앙값
     * @param basePrice84  비교 월의 전체 84㎡ 환산 평균가 (참고용 수준값, rate 계산에는 쓰지 않음)
     * @param pairedGroups 두 달 모두 거래가 있었던 같은 단지·평형 그룹 수
     */
    public record Change(int months, Double rate, String baseMonth, Long basePrice84, int baseSample,
                         int pairedGroups, String note) {}

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

        // 3) 기준월: 집계 완료된 달 중에서만 고른다
        YearMonth ref = null;
        for (YearMonth m = end; !m.isBefore(start); m = m.minusMonths(1)) {
            if (!o.isComplete(m)) continue;
            if (saleByMonth.getOrDefault(m, List.of()).size() >= o.minSample()) { ref = m; break; }
        }
        if (ref == null) {
            ref = saleByMonth.keySet().stream().filter(m -> !m.isAfter(end) && o.isComplete(m))
                    .max(Comparator.naturalOrder()).orElse(null);
            if (ref != null) notes.add("거래가 " + o.minSample() + "건 이상인 달이 없어 거래가 있는 가장 최근 달을 기준으로 했습니다(표본이 작아 변동이 큽니다).");
        }
        YearMonth firstOpen = null;
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            if (!o.isComplete(m)) { firstOpen = m; break; }
        }
        if (firstOpen != null) {
            notes.add(firstOpen + " 이후 거래는 신고 기한(계약 후 " + o.reportLagDays() + "일)이 지나지 않아 집계 중입니다. "
                    + "차트에는 보이지만 기준월·변동률·거래량 비교에는 쓰지 않았습니다. (신고가·하락 거래는 즉시 반영)");
        }
        if (ref != null && firstOpen != null && ref.plusMonths(1).isBefore(firstOpen)) {
            notes.add("기준월은 " + ref + "입니다. 그 이후 집계 완료된 달은 거래가 " + o.minSample() + "건 미만이었습니다.");
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
                List<Double> ratios = pairedRatios(rs, bs);
                if (ratios.size() < o.minPairs()) {
                    changes.add(new Change(n, null, base.toString(), b84, bs.size(), ratios.size(),
                            "비교 월(" + base + ") 거래 " + bs.size() + "건, 두 달 모두 거래된 같은 단지·평형 "
                                    + ratios.size() + "개로 부족(최소 " + o.minPairs() + "개)"));
                } else {
                    changes.add(new Change(n, medianDouble(ratios) - 1, base.toString(), b84, bs.size(), ratios.size(), null));
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
        notes.add("해제(취소)된 거래는 수집 단계에서 제외·삭제했습니다. 가격 수준은 전용 84㎡로 환산한 ㎡당 평균이고, "
                + "변동률은 같은 단지·같은 평형끼리 비교한 변동률의 중앙값입니다(거래 단지 구성이 바뀌어 생기는 착시를 줄이기 위함).");

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

    /** 같은 단지·평형(전용면적 반올림) 그룹별 ㎡당 평균가 비율 (ref / base). 두 달 모두 거래가 있는 그룹만 */
    static List<Double> pairedRatios(List<SaleRow> refRows, List<SaleRow> baseRows) {
        Map<String, Double> refAvg = perSqmByGroup(refRows);
        Map<String, Double> baseAvg = perSqmByGroup(baseRows);
        List<Double> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : refAvg.entrySet()) {
            Double b = baseAvg.get(e.getKey());
            if (b != null && b > 0) out.add(e.getValue() / b);
        }
        return out;
    }

    private static Map<String, Double> perSqmByGroup(List<SaleRow> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                s -> s.complexId() + ":" + Math.round(s.area()),
                Collectors.averagingDouble(s -> s.price() / s.area())));
    }

    static double medianDouble(List<Double> values) {
        List<Double> v = new ArrayList<>(values);
        Collections.sort(v);
        int n = v.size();
        return n % 2 == 1 ? v.get(n / 2) : (v.get(n / 2 - 1) + v.get(n / 2)) / 2.0;
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
