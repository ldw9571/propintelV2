package com.propintel.batch;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.complex.entity.Complex;
import com.propintel.domain.complex.entity.Transaction;
import com.propintel.domain.complex.repository.ComplexRepository;
import com.propintel.domain.complex.repository.TransactionRepository;
import com.propintel.domain.region.entity.Region;
import com.propintel.domain.region.repository.RegionRepository;
import com.propintel.infra.molit.MolitApiClient;   // ⬅️ 이 줄 추가
import com.propintel.infra.molit.dto.MolitTransactionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final MolitApiClient molitApiClient;
    private final ComplexRepository complexRepository;
    private final TransactionRepository transactionRepository;
    private final RegionRepository regionRepository;

    // 지역 코드 + 이름 전체 정의
    private static final List<String[]> ALL_REGIONS = Arrays.asList(
            // 서울 25개구
            new String[]{"11110", "종로구", "서울특별시", "37.5730", "126.9794"},
            new String[]{"11140", "중구", "서울특별시", "37.5636", "126.9976"},
            new String[]{"11170", "용산구", "서울특별시", "37.5324", "126.9904"},
            new String[]{"11200", "성동구", "서울특별시", "37.5634", "127.0369"},
            new String[]{"11215", "광진구", "서울특별시", "37.5385", "127.0823"},
            new String[]{"11230", "동대문구", "서울특별시", "37.5744", "127.0396"},
            new String[]{"11260", "중랑구", "서울특별시", "37.6063", "127.0927"},
            new String[]{"11290", "성북구", "서울특별시", "37.5894", "127.0167"},
            new String[]{"11305", "강북구", "서울특별시", "37.6396", "127.0257"},
            new String[]{"11320", "도봉구", "서울특별시", "37.6688", "127.0471"},
            new String[]{"11350", "노원구", "서울특별시", "37.6541", "127.0568"},
            new String[]{"11380", "은평구", "서울특별시", "37.6026", "126.9291"},
            new String[]{"11410", "마포구", "서울특별시", "37.5663", "126.9014"},
            new String[]{"11440", "서대문구", "서울특별시", "37.5791", "126.9368"},
            new String[]{"11470", "양천구", "서울특별시", "37.5170", "126.8664"},
            new String[]{"11500", "강서구", "서울특별시", "37.5509", "126.8496"},
            new String[]{"11530", "구로구", "서울특별시", "37.4954", "126.8874"},
            new String[]{"11545", "금천구", "서울특별시", "37.4600", "126.9001"},
            new String[]{"11560", "영등포구", "서울특별시", "37.5264", "126.8963"},
            new String[]{"11590", "동작구", "서울특별시", "37.5124", "126.9395"},
            new String[]{"11620", "관악구", "서울특별시", "37.4784", "126.9516"},
            new String[]{"11650", "서초구", "서울특별시", "37.4837", "127.0324"},
            new String[]{"11680", "강남구", "서울특별시", "37.5172", "127.0473"},
            new String[]{"11710", "송파구", "서울특별시", "37.5145", "127.1059"},
            new String[]{"11740", "강동구", "서울특별시", "37.5301", "127.1238"},
            // 경기도
            new String[]{"41110", "수원시", "경기도", "37.2636", "127.0286"},
            new String[]{"41130", "성남시", "경기도", "37.4196", "127.1267"},
            new String[]{"41150", "의정부시", "경기도", "37.7381", "127.0337"},
            new String[]{"41170", "안양시", "경기도", "37.3943", "126.9568"},
            new String[]{"41190", "부천시", "경기도", "37.5034", "126.7660"},
            new String[]{"41210", "광명시", "경기도", "37.4784", "126.8644"},
            new String[]{"41220", "평택시", "경기도", "37.9921", "127.1129"},
            new String[]{"41270", "안산시", "경기도", "37.3219", "126.8309"},
            new String[]{"41280", "고양시", "경기도", "37.6584", "126.8320"},
            new String[]{"41290", "과천시", "경기도", "37.4292", "126.9878"},
            new String[]{"41310", "구리시", "경기도", "37.5943", "127.1296"},
            new String[]{"41360", "남양주시", "경기도", "37.6360", "127.2165"},
            new String[]{"41410", "시흥시", "경기도", "37.3800", "126.8030"},
            new String[]{"41430", "군포시", "경기도", "37.3615", "126.9352"},
            new String[]{"41460", "하남시", "경기도", "37.5397", "127.2148"},
            new String[]{"41480", "용인시", "경기도", "37.2411", "127.1776"},
            new String[]{"41550", "파주시", "경기도", "37.7601", "126.7800"},
            new String[]{"41610", "김포시", "경기도", "37.6152", "126.7156"},
            new String[]{"41630", "화성시", "경기도", "37.1996", "126.8317"},
            // 지방 광역시
            new String[]{"21500", "해운대구", "부산광역시", "35.1632", "129.1637"},
            new String[]{"21710", "수영구", "부산광역시", "35.1453", "129.1133"},
            new String[]{"22200", "수성구", "대구광역시", "35.8582", "128.6305"},
            new String[]{"23500", "연수구", "인천광역시", "37.4103", "127.0448"},
            new String[]{"25200", "유성구", "대전광역시", "36.3624", "127.3561"}
    );

    // ① 지역 전체 자동 등록
    @GetMapping("/regions/init")
    public ApiResponse<?> initRegions() {
        int created = 0;
        for (String[] r : ALL_REGIONS) {
            if (regionRepository.findByCode(r[0]).isEmpty()) {
                Region region = Region.builder()
                        .code(r[0])
                        .name(r[1])
                        .city(r[2])
                        .district(r[1])
                        .lat(Double.parseDouble(r[3]))
                        .lng(Double.parseDouble(r[4]))
                        .build();
                regionRepository.save(region);
                created++;
            }
        }
        return ApiResponse.ok("지역 등록 완료: " + created + "개");
    }

    // ② 특정 지역 + 특정 기간 수집
    @GetMapping("/molit/collect")
    public ApiResponse<?> collectMolit(
            @RequestParam(defaultValue = "202405") String dealYmd,
            @RequestParam(defaultValue = "11680") String lawdCd) {
        return ApiResponse.ok(collect(dealYmd, lawdCd));
    }

    // ③ 전체 지역 + 여러 달 한번에 수집 (시간 오래 걸림)
    @GetMapping("/molit/collect-all")
    public ApiResponse<?> collectAll(
            @RequestParam(defaultValue = "6") int months) {

        List<Region> regions = regionRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();
        int totalSaved = 0;

        YearMonth current = YearMonth.now().minusMonths(1);

        for (int m = 0; m < months; m++) {
            YearMonth target = current.minusMonths(m);
            String dealYmd = target.getYear()
                    + String.format("%02d", target.getMonthValue());

            int monthlySaved = 0;
            for (Region region : regions) {
                try {
                    String res = collect(dealYmd, region.getCode());
                    log.info("수집: {} {} → {}", dealYmd, region.getName(), res);
                    // API 호출 간격 (과부하 방지)
                    Thread.sleep(300);
                    monthlySaved++;
                } catch (Exception e) {
                    log.warn("수집 실패: {} {}", dealYmd, region.getCode());
                }
            }
            result.put(dealYmd, monthlySaved + "개 지역 완료");
            totalSaved += monthlySaved;
        }

        result.put("총계", totalSaved + "건 처리");
        return ApiResponse.ok(result);
    }

    private String collect(String dealYmd, String lawdCd) {
        List<MolitTransactionDto> dtos =
                molitApiClient.fetchTransactions(dealYmd, lawdCd);

        Region region = regionRepository.findByCode(lawdCd).orElse(null);

        int saved = 0;
        int created = 0;

        for (MolitTransactionDto dto : dtos) {
            try {
                Optional<Complex> existing =
                        complexRepository.findByNameAndAddress(dto.aptName(), dto.umdNm());

                Complex complex;
                if (existing.isPresent()) {
                    complex = existing.get();
                } else {
                    complex = complexRepository.save(
                            Complex.builder()
                                    .region(region)
                                    .name(dto.aptName())
                                    .address(dto.umdNm())
                                    .buildYear(0)
                                    .totalUnits(0)
                                    .lat(0.0)
                                    .lng(0.0)
                                    .build()
                    );
                    created++;
                }

                String amountStr = dto.dealAmount().replace(",", "").trim();
                long price = Long.parseLong(amountStr) * 10000L;

                transactionRepository.save(
                        Transaction.builder()
                                .complex(complex)
                                .type(Transaction.TransactionType.SALE)
                                .areaSqm((int) Double.parseDouble(dto.excluUseAr()))
                                .floor(Integer.parseInt(dto.floor().trim()))
                                .price(price)
                                .dealDate(LocalDate.of(
                                        Integer.parseInt(dto.dealYear()),
                                        Integer.parseInt(dto.dealMonth()),
                                        Integer.parseInt(dto.dealDay().trim())))
                                .build()
                );
                saved++;
            } catch (Exception e) {
                log.warn("저장 실패: {} - {}", dto.aptName(), e.getMessage());
            }
        }
        return "단지 " + created + "개 생성, 거래 " + saved + "건 저장";
    }
}