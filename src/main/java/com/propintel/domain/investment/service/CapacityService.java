package com.propintel.domain.investment.service;

import com.propintel.domain.investment.calc.AcquisitionCostCalculator;
import com.propintel.domain.investment.calc.InvestmentAnalyzer;
import com.propintel.domain.investment.calc.LoanLimitCalculator;
import com.propintel.domain.investment.calc.LoanScheduleCalculator;
import com.propintel.domain.investment.calc.model.BorrowerType;
import com.propintel.domain.investment.calc.model.RegionInfo;
import com.propintel.domain.investment.calc.model.UsageType;
import com.propintel.domain.investment.calc.policy.PolicySnapshot;
import com.propintel.domain.investment.dto.InvestmentDtos.Capacity;
import com.propintel.domain.investment.dto.InvestmentDtos.ProfileRequest;
import com.propintel.domain.investment.entity.InvestorProfile;
import com.propintel.domain.investment.repository.InvestorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 내 자금 정보(프로필)로 "나의 투자 가능 금액"을 계산한다.
 * 실거주 매수를 가정하고, 현금 + 예상 대출로 살 수 있는 최대 가격을 이분 탐색으로 찾는다.
 */
@Service
public class CapacityService {

    private final InvestorProfileRepository profileRepo;
    private final PolicyService policyService;

    public CapacityService(InvestorProfileRepository profileRepo, PolicyService policyService) {
        this.profileRepo = profileRepo;
        this.policyService = policyService;
    }

    @Transactional
    public InvestorProfile profile() {
        return profileRepo.findFirstByOrderByIdAsc().orElseGet(() -> profileRepo.save(InvestorProfile.defaults()));
    }

    @Transactional
    public InvestorProfile update(ProfileRequest r) {
        policyService.region(r.targetRegionCode()); // 존재 확인
        InvestorProfile p = profile();
        p.update(r.cash(), r.monthlyIncome(), r.existingLoanBalance(), r.existingAnnualDebtService(), r.housesOwned(),
                r.firstTimeBuyer(), r.targetRegionCode(), r.loanRate(), r.loanTermYears(), r.repaymentMethod(), r.rateType());
        return p;
    }

    @Transactional
    public Capacity capacity() {
        InvestorProfile p = profile();
        PolicySnapshot snap = policyService.snapshot(null);
        RegionInfo region = policyService.regionInfo(p.getTargetRegionCode());
        BorrowerType borrower = BorrowerType.of(p.getHousesOwned(), p.isFirstTimeBuyer(), false);
        int housesAfter = p.getHousesOwned() + 1;
        long annualIncome = p.getMonthlyIncome() * 12;

        long lo = 0, hi = 10_000_000_000L; // 0 ~ 100억
        LoanLimitCalculator.Result best = null;
        AcquisitionCostCalculator.Result bestAcq = null;
        for (int i = 0; i < 50; i++) {
            long mid = (lo + hi) / 2;
            if (mid <= 0) { lo = 0; break; }
            LoanLimitCalculator.Result lim = limit(snap, region, p, borrower, annualIncome, mid);
            AcquisitionCostCalculator.Result acq = AcquisitionCostCalculator.calculate(snap, mid, 84.9, housesAfter,
                    region.regulated(), p.isFirstTimeBuyer() && p.getHousesOwned() == 0, 0);
            if (p.getCash() + lim.maxLoan() >= mid + acq.total()) { lo = mid; best = lim; bestAcq = acq; }
            else hi = mid;
            if (hi - lo < 1_000_000) break;
        }
        long maxPrice = (lo / 1_000_000) * 1_000_000;
        long loanAtMax = best == null ? 0 : Math.max(0, Math.min(best.maxLoan(),
                maxPrice + bestAcq.total() - p.getCash()));
        long dsrLimit = LoanLimitCalculator.dsrMaxLoan(annualIncome, p.getExistingAnnualDebtService(),
                snap.dsrRule(region.regionType()).dsrLimit(),
                p.getLoanRate() + snap.dsrRule(region.regionType()).appliedStress(p.getRateType()),
                p.getLoanTermYears(), p.getRepaymentMethod());
        long capacity = Math.max(0, Math.round((annualIncome * snap.dsrRule(region.regionType()).dsrLimit()
                - p.getExistingAnnualDebtService()) / 12.0));
        long monthly = loanAtMax > 0 ? LoanScheduleCalculator.calculate(loanAtMax, p.getLoanRate(),
                p.getLoanTermYears(), p.getRepaymentMethod()).firstMonthPayment() : 0;

        List<String> notes = new ArrayList<>();
        notes.add(region.displayName() + " 실거주 매수, 전용 84㎡ 기준 취득비용으로 계산했습니다.");
        if (best != null) notes.addAll(best.reasons());
        if (best != null && !best.allowed()) notes.add("현재 보유 주택 수 기준으로 이 지역 주담대가 불가하여 현금만으로 계산했습니다.");
        notes.add("최근 실거래가를 반영한 것이 아니라, 가진 돈과 대출 규제로 역산한 최대 매입 가능 가격입니다.");

        return new Capacity(p.getCash(), region.displayName(), borrower.label(), maxPrice, loanAtMax,
                bestAcq == null ? 0 : bestAcq.total(), p.getCash() + loanAtMax, dsrLimit, capacity, monthly,
                best == null ? "-" : best.bindingConstraint(), notes,
                best == null ? List.of() : best.sources(), InvestmentAnalyzer.DISCLAIMER);
    }

    private LoanLimitCalculator.Result limit(PolicySnapshot snap, RegionInfo region, InvestorProfile p,
                                             BorrowerType borrower, long annualIncome, long price) {
        return LoanLimitCalculator.calculate(snap, region, new LoanLimitCalculator.Input(price, borrower,
                UsageType.OWNER_OCCUPY, 0, annualIncome, p.getExistingAnnualDebtService(), p.getLoanRate(),
                p.getLoanTermYears(), p.getRepaymentMethod(), p.getRateType()));
    }
}
