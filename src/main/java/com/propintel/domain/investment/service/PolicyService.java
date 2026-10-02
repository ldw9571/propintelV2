package com.propintel.domain.investment.service;

import com.propintel.common.source.SourceInfo;
import com.propintel.domain.investment.calc.model.RegionInfo;
import com.propintel.domain.investment.calc.model.SourceMeta;
import com.propintel.domain.investment.calc.policy.*;
import com.propintel.domain.investment.entity.*;
import com.propintel.domain.investment.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * DB의 규제·세율 데이터를 기준일(asOf)로 모아 계산 엔진용 PolicySnapshot 을 만든다.
 * 같은 키의 행이 여러 개 유효하면 effective_from 이 가장 최근인 행을 쓴다.
 */
@Service
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyRegionRepository regionRepo;
    private final LoanRegulationRepository loanRepo;
    private final LoanAmountCapRepository capRepo;
    private final DsrRegulationRepository dsrRepo;
    private final RateBracketRepository bracketRepo;
    private final PolicyParameterRepository paramRepo;
    private final int staleAfterDays;

    public PolicyService(PolicyRegionRepository regionRepo, LoanRegulationRepository loanRepo,
                         LoanAmountCapRepository capRepo, DsrRegulationRepository dsrRepo,
                         RateBracketRepository bracketRepo, PolicyParameterRepository paramRepo,
                         @Value("${app.data.stale-after-days:90}") int staleAfterDays) {
        this.regionRepo = regionRepo;
        this.loanRepo = loanRepo;
        this.capRepo = capRepo;
        this.dsrRepo = dsrRepo;
        this.bracketRepo = bracketRepo;
        this.paramRepo = paramRepo;
        this.staleAfterDays = staleAfterDays;
    }

    public PolicySnapshot snapshot(LocalDate asOf) {
        LocalDate d = asOf != null ? asOf : LocalDate.now();
        return new PolicySnapshot(d,
                latest(loanRepo.findEffective(d), LoanRegulation::key).stream().map(LoanRegulation::toRule).toList(),
                latest(capRepo.findEffective(d), LoanAmountCap::key).stream().map(LoanAmountCap::toTier).toList(),
                latest(dsrRepo.findEffective(d), DsrRegulation::key).stream().map(DsrRegulation::toRule).toList(),
                latest(bracketRepo.findEffective(d), RateBracket::key).stream().map(RateBracket::toBracket).toList(),
                latest(paramRepo.findEffective(d), PolicyParameter::key).stream().map(PolicyParameter::toParam).toList());
    }

    public RegionInfo regionInfo(String code) {
        return region(code).toInfo();
    }

    public PolicyRegion region(String code) {
        return regionRepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 지역 코드입니다: " + code));
    }

    public Optional<PolicyRegion> findRegion(String code) {
        return regionRepo.findByCode(code);
    }

    public List<PolicyRegion> regions() {
        return regionRepo.findAllByOrderByIdAsc();
    }

    // ───────────── 조회용 뷰 ─────────────

    public record RegionView(String code, String sido, String sigungu, String displayName, boolean metro,
                             boolean regulated, boolean landPermitZone, boolean collectable, SourceMeta source) {
        public static RegionView of(PolicyRegion r) {
            return new RegionView(r.getCode(), r.getSido(), r.getSigungu(), r.displayName(), r.isMetro(),
                    r.isRegulated(), r.isLandPermitZone(), r.isCollectable(), r.getSource().toMeta());
        }
    }

    public record ParamView(Long id, String key, double value, String description,
                            LocalDate effectiveFrom, SourceMeta source) {}

    public record Freshness(int total, int unverified, int stale, int staleAfterDays) {}

    public record PolicyView(LocalDate asOf, List<LoanRule> loanRules, List<LoanCapTier> capTiers,
                             List<DsrRule> dsrRules, Map<String, List<Bracket>> brackets,
                             List<ParamView> parameters, Freshness freshness) {}

    public List<RegionView> regionViews() {
        return regions().stream().map(RegionView::of).toList();
    }

    public PolicyView view(LocalDate asOf) {
        PolicySnapshot s = snapshot(asOf);
        List<ParamView> params = latest(paramRepo.findEffective(s.asOf()), PolicyParameter::key).stream()
                .map(e -> new ParamView(e.getId(), e.getParamKey(), e.getParamValue(), e.getDescription(),
                        e.getEffectiveFrom(), e.getSource().toMeta()))
                .toList();
        return new PolicyView(s.asOf(), s.loanRules(), s.capTiers(), s.dsrRules(), s.brackets(), params, freshness());
    }

    public Freshness freshness() {
        List<SourceInfo> all = Stream.of(
                        loanRepo.findAll().stream().map(EffectiveRecord::getSource),
                        capRepo.findAll().stream().map(EffectiveRecord::getSource),
                        dsrRepo.findAll().stream().map(EffectiveRecord::getSource),
                        bracketRepo.findAll().stream().map(EffectiveRecord::getSource),
                        paramRepo.findAll().stream().map(EffectiveRecord::getSource),
                        regionRepo.findAll().stream().map(PolicyRegion::getSource))
                .flatMap(Function.identity()).toList();
        LocalDate limit = LocalDate.now().minusDays(staleAfterDays);
        int unverified = (int) all.stream().filter(si -> !si.isVerified()).count();
        int stale = (int) all.stream().filter(si -> si.getUpdatedAt().toLocalDate().isBefore(limit)).count();
        return new Freshness(all.size(), unverified, stale, staleAfterDays);
    }

    // ───────────── 수정 ─────────────

    public record UpdateParamRequest(double value, String sourceName, String sourceUrl,
                                     LocalDate baseDate, boolean verified, String note) {}

    @Transactional
    public ParamView updateParameter(Long id, UpdateParamRequest req) {
        if (req.sourceName() == null || req.sourceName().isBlank() || req.baseDate() == null) {
            throw new IllegalArgumentException("값을 바꿀 때는 출처(sourceName)와 기준일(baseDate)을 반드시 입력해야 합니다.");
        }
        PolicyParameter e = paramRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("파라미터를 찾을 수 없습니다: " + id));
        e.update(req.value(), new SourceInfo(req.sourceName(), req.sourceUrl(), req.baseDate(), req.verified(), req.note()));
        return new ParamView(e.getId(), e.getParamKey(), e.getParamValue(), e.getDescription(), e.getEffectiveFrom(),
                e.getSource().toMeta());
    }

    private static <T> List<T> latest(List<T> rows, Function<T, String> key) {
        Map<String, T> m = new LinkedHashMap<>();
        for (T r : rows) m.putIfAbsent(key.apply(r), r);
        return new ArrayList<>(m.values());
    }
}
