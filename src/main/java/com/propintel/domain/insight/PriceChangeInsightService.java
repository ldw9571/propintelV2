package com.propintel.domain.insight;

import com.propintel.common.util.Won;
import com.propintel.domain.investment.calc.model.SourceMeta;
import com.propintel.domain.investment.entity.EffectiveRecord;
import com.propintel.domain.investment.repository.DsrRegulationRepository;
import com.propintel.domain.investment.repository.LoanAmountCapRepository;
import com.propintel.domain.investment.repository.LoanRegulationRepository;
import com.propintel.domain.investment.service.PolicyService;
import com.propintel.domain.market.calc.MarketStatsCalculator.MonthPoint;
import com.propintel.domain.market.calc.MarketStatsCalculator.NotableTrade;
import com.propintel.domain.market.calc.MarketStatsCalculator.Stats;
import com.propintel.domain.market.service.MarketStatsService;
import com.propintel.domain.market.service.MarketStatsService.MarketSummary;
import com.propintel.domain.news.entity.NewsCategory;
import com.propintel.domain.news.service.NewsService;
import com.propintel.domain.news.service.NewsService.NewsView;
import com.propintel.infra.ai.AiTextClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Stream;

/**
 * Phase 5: 가격 변화 원인 분석 (근거 기반).
 *
 * 원칙
 * - 저장된 데이터(실거래·전세·규제 데이터·수집 뉴스)에서 "같은 기간에 함께 나타난 요인"만 나열한다.
 * - 데이터가 없는 항목은 "데이터 없음"으로 표시하고 추측하지 않는다.
 * - 관찰된 요인이 하나도 없으면 "확인된 직접적인 원인이 없습니다"라고 답한다.
 * - AI(선택)는 아래 요인 목록만 입력받아 문장으로 정리하며, 목록에 없는 내용을 추가하지 않도록 지시한다.
 */
@Service
public class PriceChangeInsightService {

    public enum FactorStatus { OBSERVED, NOT_OBSERVED, NO_DATA }

    public enum Direction { UP, DOWN, NEUTRAL }

    public record Evidence(String text, String url, String date) {}

    public record Factor(String name, FactorStatus status, Direction direction, String summary,
                         List<Evidence> evidence, String source) {}

    public record Insight(String scopeName, String regionCode, Long complexId, int months, String period,
                          Double changeRate, Long basePrice84, Long currentPrice84, int baseSample, int currentSample,
                          String headline, String conclusion, List<Factor> factors,
                          List<String> upSide, List<String> downSide,
                          Double sameSeasonLastYear, String lastYearNote,
                          String aiNarrative, String aiProvider, SourceMeta marketSource, List<String> cautions) {}

    private final MarketStatsService marketStats;
    private final NewsService newsService;
    private final PolicyService policyService;
    private final LoanRegulationRepository loanRepo;
    private final DsrRegulationRepository dsrRepo;
    private final LoanAmountCapRepository capRepo;
    private final AiTextClient ai;

    public PriceChangeInsightService(MarketStatsService marketStats, NewsService newsService, PolicyService policyService,
                                     LoanRegulationRepository loanRepo, DsrRegulationRepository dsrRepo,
                                     LoanAmountCapRepository capRepo, AiTextClient ai) {
        this.marketStats = marketStats;
        this.newsService = newsService;
        this.policyService = policyService;
        this.loanRepo = loanRepo;
        this.dsrRepo = dsrRepo;
        this.capRepo = capRepo;
        this.ai = ai;
    }

    @Transactional(readOnly = true)
    public Insight analyze(String regionCode, Long complexId, Double areaMin, Double areaMax, int months, boolean useAi) {
        if (months != 1 && months != 3 && months != 6 && months != 12) {
            throw new IllegalArgumentException("분석 기간은 1, 3, 6, 12개월 중 하나입니다.");
        }
        MarketSummary m = complexId != null
                ? marketStats.complex(complexId, areaMin, areaMax, 36)
                : marketStats.region(regionCode, areaMin, areaMax, 36);
        String code = m.regionCode();
        Stats s = m.stats();
        List<String> cautions = new ArrayList<>(List.of(
                "아래 요인은 가격 변화와 '같은 기간에 함께 나타난' 것으로, 원인이라고 단정할 수 없습니다.",
                "실거래 표본이 적으면 몇 건의 거래로 변동률이 크게 달라집니다."));

        if (s.refMonth() == null) {
            return new Insight(m.scopeName(), code, complexId, months, null, null, null, null, 0, 0,
                    "분석할 실거래 데이터가 없습니다.", "먼저 '시장 데이터'에서 이 지역의 실거래를 수집하세요.",
                    List.of(), List.of(), List.of(), null, null, null, null, m.source(), cautions);
        }

        YearMonth ref = YearMonth.parse(s.refMonth());
        YearMonth base = ref.minusMonths(months);
        var ch = s.change(months);
        Double rate = ch == null ? null : ch.rate();
        String period = base + " → " + ref;
        LocalDate from = base.atDay(1);
        LocalDate to = ref.atEndOfMonth();

        List<Factor> factors = new ArrayList<>();
        factors.add(volumeFactor(s, base, ref));
        factors.add(newHighFactor(s, from, to));
        factors.add(dropFactor(s, from, to));
        factors.add(jeonseFactor(s, base, ref));
        factors.add(regulationFactor(code, from, to));
        factors.addAll(newsFactors(code, from, to));
        factors.add(new Factor("금리 수준 변화", FactorStatus.NO_DATA, Direction.NEUTRAL,
                "금리 시계열 데이터가 아직 연동되지 않았습니다(FinanceDataProvider). 금리 관련 뉴스 항목을 참고하세요.",
                List.of(), null));
        factors.add(new Factor("주변 신규 입주 물량", FactorStatus.NO_DATA, Direction.NEUTRAL,
                "입주 물량 데이터가 아직 연동되지 않았습니다. 공급 관련 뉴스 항목을 참고하세요.", List.of(), null));

        String headline;
        if (rate == null) {
            headline = m.scopeName() + ": " + period + " 가격 변동률을 계산할 수 없습니다 (" + (ch == null ? "" : ch.note()) + ").";
        } else {
            headline = m.scopeName() + ": 최근 " + months + "개월(" + period + ") 84㎡ 환산 평균가 "
                    + Won.format(ch.basePrice84()) + " → " + Won.format(s.currentPrice84()) + " (" + Won.pct(rate) + ")";
        }

        List<Factor> observed = factors.stream().filter(f -> f.status() == FactorStatus.OBSERVED).toList();
        String conclusion = observed.isEmpty()
                ? "확인된 직접적인 원인이 없습니다."
                : "현재 확인되는 가격 변화와 함께 나타난 요인: "
                + String.join(", ", observed.stream().map(Factor::name).toList()) + ".";

        List<String> upSide = observed.stream().filter(f -> f.direction() == Direction.UP)
                .map(f -> f.name() + " — " + f.summary()).toList();
        List<String> downSide = observed.stream().filter(f -> f.direction() == Direction.DOWN)
                .map(f -> f.name() + " — " + f.summary()).toList();

        // 과거 데이터: 1년 전 같은 기간 변동률
        Double lastYear = null;
        String lastYearNote;
        Long a = price84(s, ref.minusMonths(12)), b = price84(s, base.minusMonths(12));
        if (a != null && b != null && b > 0) {
            lastYear = (double) a / b - 1;
            lastYearNote = "1년 전 같은 기간(" + base.minusMonths(12) + " → " + ref.minusMonths(12) + ") 변동률 " + Won.pct(lastYear);
        } else {
            lastYearNote = "1년 전 같은 기간의 거래가 부족해 비교할 수 없습니다.";
        }

        String narrative = null;
        if (useAi && ai.isEnabled() && rate != null) {
            narrative = narrative(headline, factors, lastYearNote);
        }

        return new Insight(m.scopeName(), code, complexId, months, period, rate,
                ch == null ? null : ch.basePrice84(), s.currentPrice84(), ch == null ? 0 : ch.baseSample(),
                s.refSampleSize(), headline, conclusion, factors, upSide, downSide, lastYear, lastYearNote,
                narrative, ai.providerName(), m.source(), cautions);
    }

    // ───────────── 요인 ─────────────

    Factor volumeFactor(Stats s, YearMonth base, YearMonth ref) {
        Integer now = count(s, ref), before = count(s, base);
        var v = s.volume();
        if (v.ratio() == null) {
            return new Factor("거래량 변화", FactorStatus.NO_DATA, Direction.NEUTRAL, "비교할 직전 거래가 부족합니다.", List.of(), null);
        }
        boolean moved = v.ratio() >= 1.3 || v.ratio() <= 0.77;
        Direction d = v.ratio() >= 1.3 ? Direction.UP : (v.ratio() <= 0.77 ? Direction.DOWN : Direction.NEUTRAL);
        String text = "기준월 " + v.refCount() + "건, 직전 6개월 평균 " + String.format("%.1f", v.baselineAvg())
                + "건 (" + String.format("%.1f", v.ratio()) + "배, " + v.trend() + "). " + base + " " + (before == null ? 0 : before)
                + "건 → " + ref + " " + (now == null ? 0 : now) + "건";
        return new Factor("거래량 변화", moved ? FactorStatus.OBSERVED : FactorStatus.NOT_OBSERVED, d, text, List.of(),
                MarketStatsService.SOURCE_NAME);
    }

    Factor newHighFactor(Stats s, LocalDate from, LocalDate to) {
        int cnt = s.monthly().stream().filter(p -> within(p.month(), from, to)).mapToInt(MonthPoint::newHighCount).sum();
        List<Evidence> ev = s.newHighs().stream().filter(t -> !t.date().isBefore(from) && !t.date().isAfter(to)).limit(5)
                .map(t -> new Evidence(t.complexName() + " " + Math.round(t.area()) + "㎡ " + t.floor() + "층 " + Won.format(t.price())
                        + " (이전 최고 " + Won.format(t.referencePrice()) + ", " + Won.pct(t.diffRate()) + ")", null, t.date().toString()))
                .toList();
        return new Factor("신고가 발생", cnt > 0 ? FactorStatus.OBSERVED : FactorStatus.NOT_OBSERVED, Direction.UP,
                cnt > 0 ? "기간 중 신고가 " + cnt + "건" : "기간 중 신고가 없음", ev, MarketStatsService.SOURCE_NAME);
    }

    Factor dropFactor(Stats s, LocalDate from, LocalDate to) {
        int cnt = s.monthly().stream().filter(p -> within(p.month(), from, to)).mapToInt(MonthPoint::dropCount).sum();
        int total = s.monthly().stream().filter(p -> within(p.month(), from, to)).mapToInt(MonthPoint::saleCount).sum();
        double share = total == 0 ? 0 : (double) cnt / total;
        boolean observed = cnt > 0 && share >= 0.2;
        return new Factor("하락 거래(직전 대비 5% 이상 낮은 거래)", observed ? FactorStatus.OBSERVED : FactorStatus.NOT_OBSERVED,
                Direction.DOWN, "기간 거래 " + total + "건 중 " + cnt + "건 (" + Math.round(share * 100) + "%)", List.of(),
                MarketStatsService.SOURCE_NAME);
    }

    Factor jeonseFactor(Stats s, YearMonth base, YearMonth ref) {
        Long now = jeonse84(s, ref), before = jeonse84(s, base);
        if (now == null || before == null || before == 0) {
            return new Factor("전세가격 변화", FactorStatus.NO_DATA, Direction.NEUTRAL,
                    "비교할 전세 거래가 부족합니다(전월세 실거래 수집 필요).", List.of(), null);
        }
        double r = (double) now / before - 1;
        boolean moved = Math.abs(r) >= 0.02;
        String text = "전세 84㎡ 환산 " + Won.format(before) + " → " + Won.format(now) + " (" + Won.pct(r) + ")"
                + (s.jeonse().jeonseRatio() == null ? "" : ", 최근 전세가율 " + Math.round(s.jeonse().jeonseRatio() * 100) + "%");
        return new Factor("전세가격 변화", moved ? FactorStatus.OBSERVED : FactorStatus.NOT_OBSERVED,
                r > 0 ? Direction.UP : Direction.DOWN, text, List.of(), "국토교통부 아파트 전월세 실거래가");
    }

    Factor regulationFactor(String regionCode, LocalDate from, LocalDate to) {
        List<Evidence> ev = new ArrayList<>();
        Stream.of(loanRepo.findAll().stream().map(e -> (EffectiveRecord) e),
                        dsrRepo.findAll().stream().map(e -> (EffectiveRecord) e),
                        capRepo.findAll().stream().map(e -> (EffectiveRecord) e))
                .flatMap(x -> x)
                .filter(e -> !e.getEffectiveFrom().isBefore(from) && !e.getEffectiveFrom().isAfter(to))
                .map(e -> e.getSource().getSourceName() + "|" + e.getSource().getSourceUrl() + "|" + e.getEffectiveFrom())
                .distinct()
                .forEach(k -> {
                    String[] p = k.split("\\|");
                    ev.add(new Evidence("시행: " + p[0], "null".equals(p[1]) ? null : p[1], p[2]));
                });
        if (regionCode != null) {
            policyService.findRegion(regionCode).ifPresent(r -> {
                LocalDate d = r.getSource().getBaseDate();
                if (r.isRegulated() && d != null && !d.isBefore(from) && !d.isAfter(to)) {
                    ev.add(new Evidence(r.displayName() + " 규제지역" + (r.isLandPermitZone() ? "·토지거래허가구역" : "")
                            + " 지정: " + r.getSource().getSourceName(), r.getSource().getSourceUrl(), d.toString()));
                }
            });
        }
        return new Factor("대출·규제 변화", ev.isEmpty() ? FactorStatus.NOT_OBSERVED : FactorStatus.OBSERVED, Direction.NEUTRAL,
                ev.isEmpty() ? "기간 중 저장된 규제 데이터의 시행일 변화 없음" : "기간 중 시행된 규제 " + ev.size() + "건 (방향은 지역·차주에 따라 다름)",
                ev, "기준 데이터(규제·세율) — 미검증 항목 포함");
    }

    List<Factor> newsFactors(String regionCode, LocalDate from, LocalDate to) {
        LocalDateTime f = from.atStartOfDay(), t = to.plusDays(1).atStartOfDay();
        List<NewsView> regional = regionCode == null ? List.of() : newsService.regionNewsBetween(regionCode, f, t);
        List<NewsView> national = newsService.nationalNewsBetween(EnumSet.of(NewsCategory.POLICY, NewsCategory.RATE, NewsCategory.SUPPLY), f, t);
        List<Factor> out = new ArrayList<>();
        out.add(newsFactor("교통·개발 관련 보도", regional, NewsCategory.TRANSPORT));
        out.add(newsFactor("재건축·재개발·정비사업 관련 보도", regional, NewsCategory.REDEVELOPMENT));
        out.add(newsFactor("입주·분양·공급 관련 보도", concat(regional, national), NewsCategory.SUPPLY));
        out.add(newsFactor("정책·규제 관련 보도", concat(regional, national), NewsCategory.POLICY));
        out.add(newsFactor("금리 관련 보도", concat(regional, national), NewsCategory.RATE));
        return out;
    }

    Factor newsFactor(String name, List<NewsView> news, NewsCategory cat) {
        List<NewsView> hit = news.stream().filter(n -> n.category().equals(cat.name())).toList();
        if (news.isEmpty()) {
            return new Factor(name, FactorStatus.NO_DATA, Direction.NEUTRAL, "기간 중 수집된 뉴스가 없습니다(뉴스 수집은 등록 이후부터 쌓입니다).", List.of(), null);
        }
        List<Evidence> ev = hit.stream().limit(5)
                .map(n -> new Evidence((n.mock() ? "[샘플] " : "") + n.title() + (n.publisher() == null ? "" : " — " + n.publisher()),
                        n.url(), n.publishedAt().toLocalDate().toString()))
                .toList();
        return new Factor(name, hit.isEmpty() ? FactorStatus.NOT_OBSERVED : FactorStatus.OBSERVED, Direction.NEUTRAL,
                hit.isEmpty() ? "기간 중 관련 보도 없음" : "기간 중 관련 보도 " + hit.size() + "건 (기사 내용이 가격에 반영됐는지는 확인되지 않음)",
                ev, hit.isEmpty() ? null : hit.get(0).sourceName());
    }

    // ───────────── AI 정리 (선택) ─────────────

    String narrative(String headline, List<Factor> factors, String lastYearNote) {
        StringBuilder sb = new StringBuilder();
        for (Factor f : factors) {
            sb.append("- ").append(f.name()).append(" [").append(f.status()).append("]: ").append(f.summary()).append('\n');
            for (Evidence e : f.evidence()) sb.append("    · ").append(e.date()).append(' ').append(e.text()).append('\n');
        }
        String prompt = """
                당신은 부동산 데이터를 설명하는 조수입니다. 아래 '확인된 데이터'만 사용해 한국어 4~6문장으로 정리하세요.
                규칙:
                - 목록에 없는 사실, 수치, 지명, 전망을 추가하지 마세요.
                - OBSERVED 항목만 "함께 나타난 요인"으로 언급하고, 원인이라고 단정하지 마세요("~와 같은 시기에 나타났습니다").
                - NO_DATA 항목은 "데이터가 없어 확인할 수 없다"고만 언급하세요.
                - OBSERVED 항목이 없으면 "확인된 직접적인 원인이 없습니다"라고 쓰세요.
                - 매수·매도 추천을 하지 마세요.

                [가격 변화] %s
                [과거 비교] %s
                [확인된 데이터]
                %s
                """.formatted(headline, lastYearNote, sb);
        return ai.complete(prompt);
    }

    // ───────────── helpers ─────────────

    private static Integer count(Stats s, YearMonth ym) {
        return s.monthly().stream().filter(p -> p.month().equals(ym.toString())).map(MonthPoint::saleCount).findFirst().orElse(null);
    }

    private static Long price84(Stats s, YearMonth ym) {
        return s.monthly().stream().filter(p -> p.month().equals(ym.toString()) && p.saleCount() >= 1)
                .map(MonthPoint::salePrice84).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static Long jeonse84(Stats s, YearMonth ym) {
        return s.monthly().stream().filter(p -> p.month().equals(ym.toString()))
                .map(MonthPoint::jeonsePrice84).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static boolean within(String month, LocalDate from, LocalDate to) {
        YearMonth m = YearMonth.parse(month);
        return !m.isBefore(YearMonth.from(from)) && !m.isAfter(YearMonth.from(to));
    }

    private static List<NewsView> concat(List<NewsView> a, List<NewsView> b) {
        List<NewsView> l = new ArrayList<>(a);
        l.addAll(b);
        return l;
    }
}
