package com.propintel.domain.report.service;

import com.propintel.domain.complex.repository.TransactionRepository;
import com.propintel.domain.region.entity.Region;
import com.propintel.domain.region.repository.RegionRepository;
import com.propintel.infra.openai.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private final GeminiClient  openAiClient;
    private final RegionRepository regionRepository;
    private final TransactionRepository transactionRepository;

    public Map<String, Object> analyzeRegion(String regionCode) {
        Region region = regionRepository.findByCode(regionCode)
                .orElseThrow(() -> new IllegalArgumentException("지역 없음: " + regionCode));

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(12);
        List<Long> prices = transactionRepository.findPricesByDateRange(start, end);

        long avgPrice = prices.isEmpty() ? 0 :
                (long) prices.stream().mapToLong(Long::longValue).average().orElse(0) / 10000;
        long maxPrice = prices.isEmpty() ? 0 :
                prices.stream().mapToLong(Long::longValue).max().orElse(0) / 10000;
        long minPrice = prices.isEmpty() ? 0 :
                prices.stream().mapToLong(Long::longValue).min().orElse(0) / 10000;
        long count = prices.size();

        String prompt = String.format("""
                당신은 한국 부동산 전문 투자 분석가입니다.
                아래 데이터를 바탕으로 투자 분석 리포트를 작성해주세요.

                [지역 정보]
                - 지역: %s %s
                - 분석 기간: 최근 12개월
                - 거래 건수: %d건
                - 평균 매매가: %s만원
                - 최고가: %s만원
                - 최저가: %s만원

                다음 항목을 JSON 형식으로만 답변해주세요 (다른 텍스트 없이):
                {
                  "investScore": 투자점수(0-100),
                  "summary": "한줄 요약",
                  "advantages": ["장점1", "장점2", "장점3"],
                  "risks": ["위험요소1", "위험요소2"],
                  "priceOutlook": "향후 1~3년 가격 전망",
                  "expectedReturn": "예상 수익률(예: 연 3~5%%)",
                  "newbieAdvice": "신혼부부/실거주자를 위한 조언",
                  "bestTimeToBuy": "매수 적기 판단"
                }
                """,
                region.getCity(), region.getName(),
                count,
                String.format("%,d", avgPrice),
                String.format("%,d", maxPrice),
                String.format("%,d", minPrice)
        );

        String gptResponse = openAiClient.chat(prompt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("region", Map.of(
                "code", region.getCode(),
                "name", region.getName(),
                "city", region.getCity()
        ));
        result.put("stats", Map.of(
                "avgPrice", avgPrice,
                "maxPrice", maxPrice,
                "minPrice", minPrice,
                "count", count
        ));
        result.put("ai", parseGptJson(gptResponse));
        return result;
    }

    public Map<String, Object> recommendForNewlyweds(int budget, String preference) {
        List<Region> regions = regionRepository.findAll();

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(6);

        List<Map<String, Object>> affordable = new ArrayList<>();
        for (Region region : regions) {
            List<Long> prices = transactionRepository.findPricesByDateRange(start, end);
            if (prices.isEmpty()) continue;
            long avg = (long) prices.stream().mapToLong(Long::longValue).average().orElse(0) / 10000;
            if (avg <= budget) {
                affordable.add(new LinkedHashMap<>(Map.of(
                        "name", region.getCity() + " " + region.getName(),
                        "code", region.getCode(),
                        "avgPrice", avg,
                        "count", prices.size()
                )));
            }
        }

        affordable.sort((a, b) -> Long.compare((long)b.get("count"), (long)a.get("count")));

        String regionList = affordable.stream().limit(10)
                .map(r -> r.get("name") + "(평균 " + String.format("%,d", r.get("avgPrice")) + "만원)")
                .collect(Collectors.joining(", "));

        String prompt = String.format("""
                당신은 한국 부동산 전문가입니다. 신혼부부를 위한 맞춤 부동산 추천을 해주세요.

                [조건]
                - 예산: %s만원
                - 선호 유형: %s
                - 예산 내 가능 지역: %s

                다음 JSON 형식으로만 답변해주세요 (다른 텍스트 없이):
                {
                  "top3": [
                    {
                      "rank": 1,
                      "region": "지역명",
                      "reason": "추천 이유",
                      "avgPrice": "평균가격",
                      "pros": ["장점1", "장점2"],
                      "cons": ["단점1"],
                      "newlywedScore": 신혼부부 적합도(0-100)
                    }
                  ],
                  "budgetAdvice": "예산 관련 조언",
                  "timingAdvice": "지금 매수/전세 타이밍 조언",
                  "futureValue": "5년 후 예상 가치",
                  "checkList": ["체크리스트1", "체크리스트2", "체크리스트3"]
                }
                """,
                String.format("%,d", budget),
                preference,
                regionList.isEmpty() ? "조건에 맞는 지역 없음" : regionList
        );

        String gptResponse = openAiClient.chat(prompt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("budget", budget);
        result.put("preference", preference);
        result.put("affordableRegions", affordable);
        result.put("ai", parseGptJson(gptResponse));
        return result;
    }

    public Map<String, Object> rankAllRegions() {
        List<Region> regions = regionRepository.findAll();

        LocalDate end = LocalDate.now();
        LocalDate start3m = end.minusMonths(3);
        LocalDate start6m = end.minusMonths(6);

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Region region : regions) {
            List<Long> recent = transactionRepository.findPricesByDateRange(start3m, end);
            List<Long> prev = transactionRepository.findPricesByDateRange(start6m, start3m);
            if (recent.isEmpty()) continue;

            long avgRecent = (long) recent.stream().mapToLong(Long::longValue).average().orElse(0) / 10000;
            long avgPrev = prev.isEmpty() ? avgRecent :
                    (long) prev.stream().mapToLong(Long::longValue).average().orElse(0) / 10000;
            double changeRate = avgPrev == 0 ? 0 :
                    Math.round((double)(avgRecent - avgPrev) / avgPrev * 1000.0) / 10.0;

            stats.add(new LinkedHashMap<>(Map.of(
                    "code", region.getCode(),
                    "name", region.getCity() + " " + region.getName(),
                    "avgPrice", avgRecent,
                    "changeRate", changeRate,
                    "count", recent.size()
            )));
        }

        stats.sort((a, b) -> Double.compare((double)b.get("changeRate"), (double)a.get("changeRate")));

        String top10 = stats.stream().limit(10)
                .map(r -> r.get("name") + "(상승률 " + r.get("changeRate") + "%, 평균 "
                        + String.format("%,d", r.get("avgPrice")) + "만원)")
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                당신은 한국 부동산 전문 투자 분석가입니다.
                최근 3개월 실거래 데이터 기반 TOP10 지역을 분석해주세요.

                [TOP10 상승 지역]
                %s

                다음 JSON으로만 답변해주세요 (다른 텍스트 없이):
                {
                  "hotRegions": ["주목할 지역1", "주목할 지역2", "주목할 지역3"],
                  "marketTrend": "전체 시장 트렌드 분석",
                  "investStrategy": "현재 시점 투자 전략",
                  "warningRegions": ["주의할 지역1", "주의할 지역2"],
                  "outlook3month": "3개월 전망",
                  "outlook1year": "1년 전망"
                }
                """, top10);

        String gptResponse = openAiClient.chat(prompt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ranking", stats);
        result.put("ai", parseGptJson(gptResponse));
        return result;
    }

    private Object parseGptJson(String response) {
        try {
            String clean = response
                    .replaceAll("(?s)```json", "")
                    .replaceAll("(?s)```", "")
                    .trim();
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(clean, Object.class);
        } catch (Exception e) {
            log.warn("GPT JSON 파싱 실패: {}", e.getMessage());
            return response;
        }
    }
}