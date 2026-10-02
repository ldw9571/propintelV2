package com.propintel.domain.investment.calc.policy;

import com.propintel.domain.investment.calc.model.BorrowerType;
import com.propintel.domain.investment.calc.model.RegionType;
import com.propintel.domain.investment.calc.model.SourceMeta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 특정 기준일에 유효한 규제·세율 데이터 묶음.
 * 계산 엔진은 이 스냅샷만 보고 계산하므로, DB 값을 바꾸면 코드 수정 없이 계산이 바뀐다.
 */
public final class PolicySnapshot {

    private final LocalDate asOf;
    private final List<LoanRule> loanRules;
    private final List<LoanCapTier> capTiers;
    private final List<DsrRule> dsrRules;
    private final Map<String, List<Bracket>> brackets;
    private final Map<String, PolicyParam> params;

    public PolicySnapshot(LocalDate asOf, List<LoanRule> loanRules, List<LoanCapTier> capTiers,
                          List<DsrRule> dsrRules, List<Bracket> brackets, List<PolicyParam> params) {
        this.asOf = asOf;
        this.loanRules = List.copyOf(loanRules);
        this.capTiers = List.copyOf(capTiers);
        this.dsrRules = List.copyOf(dsrRules);
        this.brackets = brackets.stream()
                .sorted(Comparator.comparingLong(Bracket::over))
                .collect(Collectors.groupingBy(Bracket::tableCode));
        this.params = params.stream().collect(Collectors.toMap(PolicyParam::key, Function.identity(), (a, b) -> b));
    }

    public LocalDate asOf() { return asOf; }
    public List<LoanRule> loanRules() { return loanRules; }
    public List<LoanCapTier> capTiers() { return capTiers; }
    public List<DsrRule> dsrRules() { return dsrRules; }
    public Map<String, PolicyParam> params() { return params; }
    public Map<String, List<Bracket>> brackets() { return brackets; }

    public Optional<LoanRule> loanRule(RegionType region, BorrowerType borrower) {
        return loanRules.stream()
                .filter(r -> r.regionType() == region && r.borrowerType() == borrower)
                .findFirst();
    }

    public Optional<LoanCapTier> capTier(RegionType region, long price) {
        return capTiers.stream()
                .filter(t -> t.regionType() == region && t.matches(price))
                .findFirst();
    }

    public DsrRule dsrRule(RegionType region) {
        return dsrRules.stream().filter(r -> r.regionType() == region).findFirst()
                .orElseThrow(() -> new IllegalStateException("DSR 규칙 데이터가 없습니다: " + region));
    }

    public List<Bracket> brackets(String tableCode) {
        List<Bracket> list = brackets.get(tableCode);
        if (list == null || list.isEmpty()) {
            throw new IllegalStateException("요율표 데이터가 없습니다: " + tableCode);
        }
        return list;
    }

    public double param(String key) {
        return paramMeta(key).value();
    }

    public PolicyParam paramMeta(String key) {
        PolicyParam p = params.get(key);
        if (p == null) throw new IllegalStateException("정책 파라미터가 없습니다: " + key);
        return p;
    }

    /** 계산에 사용된 출처 목록 (중복 제거) */
    public static List<SourceMeta> distinct(List<SourceMeta> metas) {
        List<SourceMeta> out = new ArrayList<>();
        for (SourceMeta m : metas) {
            if (m != null && out.stream().noneMatch(o -> o.equals(m))) out.add(m);
        }
        return out;
    }
}
