package com.propintel.domain.investment.service;

import com.propintel.domain.investment.entity.PolicyRegion;
import com.propintel.domain.investment.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 앱 시작 시 규제·세율·지역 기본 데이터를 넣는다.
 * 테이블이 비어 있을 때만 넣으므로(지역은 없는 코드만 추가), 화면에서 수정한 값은 덮어쓰지 않는다.
 */
@Component
@Order(1)
public class PolicySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicySeeder.class);

    private final PolicyRegionRepository regionRepo;
    private final LoanRegulationRepository loanRepo;
    private final LoanAmountCapRepository capRepo;
    private final DsrRegulationRepository dsrRepo;
    private final RateBracketRepository bracketRepo;
    private final PolicyParameterRepository paramRepo;

    public PolicySeeder(PolicyRegionRepository regionRepo, LoanRegulationRepository loanRepo,
                        LoanAmountCapRepository capRepo, DsrRegulationRepository dsrRepo,
                        RateBracketRepository bracketRepo, PolicyParameterRepository paramRepo) {
        this.regionRepo = regionRepo;
        this.loanRepo = loanRepo;
        this.capRepo = capRepo;
        this.dsrRepo = dsrRepo;
        this.bracketRepo = bracketRepo;
        this.paramRepo = paramRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<String> existing = regionRepo.findAll().stream().map(PolicyRegion::getCode).collect(Collectors.toSet());
        int added = 0;
        for (PolicyRegion r : PolicySeedData.regions()) {
            if (!existing.contains(r.getCode())) { regionRepo.save(r); added++; }
        }
        if (loanRepo.count() == 0) loanRepo.saveAll(PolicySeedData.loanRules());
        if (capRepo.count() == 0) capRepo.saveAll(PolicySeedData.caps());
        if (dsrRepo.count() == 0) dsrRepo.saveAll(PolicySeedData.dsr());
        if (bracketRepo.count() == 0) bracketRepo.saveAll(PolicySeedData.brackets());
        Set<String> keys = paramRepo.findAll().stream().map(p -> p.getParamKey()).collect(Collectors.toSet());
        PolicySeedData.params().stream().filter(p -> !keys.contains(p.getParamKey())).forEach(paramRepo::save);
        log.info("[기준 데이터] 정책 지역 {}곳 추가, 규제·세율 데이터 확인 완료", added);
    }
}
