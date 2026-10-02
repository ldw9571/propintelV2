package com.propintel.domain.market.service;

import com.propintel.domain.complex.entity.Complex;
import com.propintel.domain.complex.entity.Transaction;
import com.propintel.domain.investment.entity.PolicyRegion;
import com.propintel.domain.investment.service.PolicyService;
import com.propintel.domain.market.entity.CollectionLog;
import com.propintel.domain.market.entity.RentTransaction;
import com.propintel.domain.market.repository.*;
import com.propintel.domain.region.entity.Region;
import com.propintel.domain.region.repository.RegionRepository;
import com.propintel.infra.molit.MolitRawClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 국토부 매매·전월세 실거래를 수집해 저장한다.
 * - 매매: 기존 transaction 테이블(type=SALE)에 저장, 해제 거래 제외, 중복 저장 방지
 * - 전월세: rent_transaction 테이블에 저장
 * - 수집 이력(collection_log)에 월·유형별 건수와 오류를 남긴다
 */
@Service
public class TransactionIngestService {

    private static final Logger log = LoggerFactory.getLogger(TransactionIngestService.class);
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");

    public enum DataType { SALE, RENT }

    /** canceled: 응답에 포함된 해제 거래 수, removedCanceled: 그중 이미 저장돼 있어 삭제한 거래 수 */
    public record MonthResult(String dealYm, DataType type, int fetched, int saved, int duplicates,
                              int canceled, int removedCanceled, String error) {}

    public record CollectResult(String regionCode, String regionName, List<MonthResult> months,
                                int totalSaved, int errors) {}

    private final MolitRawClient client;
    private final PolicyService policyService;
    private final RegionRepository regionRepository;
    private final ComplexLookupRepository complexRepo;
    private final SaleTradeRepository saleRepo;
    private final RentTransactionRepository rentRepo;
    private final CollectionLogRepository logRepo;
    private final TransactionTemplate tx;

    public TransactionIngestService(MolitRawClient client, PolicyService policyService,
                                    RegionRepository regionRepository, ComplexLookupRepository complexRepo,
                                    SaleTradeRepository saleRepo, RentTransactionRepository rentRepo,
                                    CollectionLogRepository logRepo, PlatformTransactionManager txManager) {
        this.client = client;
        this.policyService = policyService;
        this.regionRepository = regionRepository;
        this.complexRepo = complexRepo;
        this.saleRepo = saleRepo;
        this.rentRepo = rentRepo;
        this.logRepo = logRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    /** 최근 months개월(이번 달 포함) 수집 */
    public CollectResult collectRecent(String regionCode, int months, Set<DataType> types) {
        if (months < 1 || months > 60) throw new IllegalArgumentException("수집 기간은 1~60개월입니다.");
        PolicyRegion pr = policyService.region(regionCode);
        if (!pr.isCollectable()) {
            throw new IllegalArgumentException(pr.displayName() + "은(는) 대표 코드라 국토부 API로 수집할 수 없습니다. 구 단위 지역을 선택하세요.");
        }
        Region region = tx.execute(s -> ensureRegion(pr));
        List<MonthResult> results = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 0; i < months; i++) {
            String ym = now.minusMonths(i).format(YM);
            if (types.contains(DataType.SALE)) results.add(collectSales(region, regionCode, ym));
            if (types.contains(DataType.RENT)) results.add(collectRents(region, regionCode, ym));
        }
        int saved = results.stream().mapToInt(MonthResult::saved).sum();
        int errors = (int) results.stream().filter(r -> r.error() != null).count();
        log.info("[실거래 수집] {} {}개월 → 저장 {}건, 오류 {}건", pr.displayName(), months, saved, errors);
        return new CollectResult(regionCode, pr.displayName(), results, saved, errors);
    }

    private MonthResult collectSales(Region region, String code, String ym) {
        try {
            List<MolitRawClient.SaleItem> items = client.fetchSales(code, ym);
            int[] c = tx.execute(s -> {
                int saved = 0, dup = 0, canceled = 0, removed = 0;
                Map<String, Complex> cache = new HashMap<>();
                for (MolitRawClient.SaleItem it : items) {
                    if (it.canceled()) {
                        canceled++;
                        removed += removeCanceled(code, it);
                        continue;
                    }
                    if (it.aptName() == null || it.year() == 0) continue;
                    Complex cx = complex(cache, region, code, it.aptName(), it.umdName(), it.buildYear());
                    LocalDate d = LocalDate.of(it.year(), it.month(), it.day());
                    int area = (int) it.area();
                    if (saleRepo.existsByComplexIdAndTypeAndDealDateAndFloorAndAreaSqmAndPrice(
                            cx.getId(), Transaction.TransactionType.SALE, d, it.floor(), area, it.priceWon())) {
                        dup++;
                        continue;
                    }
                    saleRepo.save(Transaction.builder().complex(cx).type(Transaction.TransactionType.SALE)
                            .areaSqm(area).floor(it.floor()).price(it.priceWon()).dealDate(d).build());
                    saved++;
                }
                return new int[]{saved, dup, canceled, removed};
            });
            if (c[3] > 0) log.info("[실거래 수집] {} {} 해제 신고된 기존 거래 {}건 삭제", code, ym, c[3]);
            logRepo.save(new CollectionLog(code, ym, "SALE", items.size(), c[0], c[1], c[2], null));
            return new MonthResult(ym, DataType.SALE, items.size(), c[0], c[1], c[2], c[3], null);
        } catch (RuntimeException e) {
            log.warn("매매 수집 실패 {} {}: {}", code, ym, e.getMessage());
            logRepo.save(new CollectionLog(code, ym, "SALE", 0, 0, 0, 0, e.getMessage()));
            return new MonthResult(ym, DataType.SALE, 0, 0, 0, 0, 0, e.getMessage());
        }
    }

    private MonthResult collectRents(Region region, String code, String ym) {
        try {
            List<MolitRawClient.RentItem> items = client.fetchRents(code, ym);
            int[] c = tx.execute(s -> {
                int saved = 0, dup = 0;
                Map<String, Complex> cache = new HashMap<>();
                for (MolitRawClient.RentItem it : items) {
                    if (it.aptName() == null || it.year() == 0) continue;
                    Complex cx = complex(cache, region, code, it.aptName(), it.umdName(), it.buildYear());
                    LocalDate d = LocalDate.of(it.year(), it.month(), it.day());
                    if (rentRepo.existsByComplexIdAndDealDateAndFloorAndAreaSqmAndDepositAndMonthlyRent(
                            cx.getId(), d, it.floor(), it.area(), it.depositWon(), it.monthlyRentWon())) {
                        dup++;
                        continue;
                    }
                    rentRepo.save(new RentTransaction(cx, code, it.area(), it.floor(), it.depositWon(),
                            it.monthlyRentWon(), d, it.contractType(), it.contractTerm(), it.preDepositWon(),
                            it.preMonthlyRentWon()));
                    saved++;
                }
                return new int[]{saved, dup};
            });
            logRepo.save(new CollectionLog(code, ym, "RENT", items.size(), c[0], c[1], 0, null));
            return new MonthResult(ym, DataType.RENT, items.size(), c[0], c[1], 0, 0, null);
        } catch (RuntimeException e) {
            log.warn("전월세 수집 실패 {} {}: {}", code, ym, e.getMessage());
            logRepo.save(new CollectionLog(code, ym, "RENT", 0, 0, 0, 0, e.getMessage()));
            return new MonthResult(ym, DataType.RENT, 0, 0, 0, 0, 0, e.getMessage());
        }
    }

    /**
     * 해제(취소) 신고된 거래가 이미 저장돼 있으면 삭제한다. (신고가 후 취소되는 "신고가 띄우기" 거래가 남지 않도록)
     * 단지가 아직 없으면 새로 만들지 않는다.
     */
    private int removeCanceled(String code, MolitRawClient.SaleItem it) {
        if (it.aptName() == null || it.year() == 0) return 0;
        String address = it.umdName() == null ? "" : it.umdName();
        Optional<Complex> cx = complexRepo.findFirstByNameAndAddressAndRegion_CodeOrderByIdAsc(it.aptName(), address, code)
                .or(() -> complexRepo.findFirstByNameAndAddressOrderByIdAsc(it.aptName(), address));
        if (cx.isEmpty()) return 0;
        long n = saleRepo.deleteByComplexIdAndTypeAndDealDateAndFloorAndAreaSqmAndPrice(
                cx.get().getId(), Transaction.TransactionType.SALE, LocalDate.of(it.year(), it.month(), it.day()),
                it.floor(), (int) it.area(), it.priceWon());
        return (int) n;
    }

    private Complex complex(Map<String, Complex> cache, Region region, String code, String name, String umd,
                            Integer buildYear) {
        String address = umd == null ? "" : umd;
        return cache.computeIfAbsent(name + "|" + address, k ->
                complexRepo.findFirstByNameAndAddressAndRegion_CodeOrderByIdAsc(name, address, code)
                        .or(() -> complexRepo.findFirstByNameAndAddressOrderByIdAsc(name, address))
                        .orElseGet(() -> complexRepo.save(Complex.builder().region(region).name(name)
                                .address(address).buildYear(buildYear == null ? 0 : buildYear)
                                .totalUnits(0).build())));
    }

    private Region ensureRegion(PolicyRegion pr) {
        return regionRepository.findByCode(pr.getCode()).orElseGet(() -> regionRepository.save(Region.builder()
                .code(pr.getCode())
                .name(pr.getSigungu() == null ? pr.getSido() : pr.getSigungu())
                .city(pr.getSido())
                .district(pr.getSigungu())
                .build()));
    }
}
