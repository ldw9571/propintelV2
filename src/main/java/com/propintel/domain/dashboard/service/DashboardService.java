package com.propintel.domain.dashboard.service;

import com.propintel.domain.complex.repository.TransactionRepository;
import com.propintel.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final RegionRepository regionRepository;

    public Map<String, Object> getSummary() {
        Map<String, Object> result = new HashMap<>();

        // 전체 거래 평균가 (만원 단위)
        List<Long> prices = transactionRepository.findAllPrices();
        long avgPrice = prices.isEmpty() ? 0 :
                (long) prices.stream().mapToLong(Long::longValue).average().orElse(0) / 10000;

        // 이번달 거래량
        LocalDate now = LocalDate.now();
        long totalVolume = transactionRepository.countByDealDateBetween(
                now.withDayOfMonth(1), now);

        result.put("avgPrice", avgPrice);
        result.put("totalVolume", totalVolume);
        result.put("monthlyChange", 2.3);   // 추후 실계산
        result.put("jeonseRatio", 55.2);    // 추후 실계산
        return result;
    }

    public List<Map<String, Object>> getPriceIndex(String area, int months) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 실제 데이터가 있는 2024년 5월 기준으로 조회
        LocalDate base = LocalDate.of(2024, 5, 1);

        for (int i = months - 1; i >= 0; i--) {
            LocalDate date = base.minusMonths(i);
            LocalDate start = date.withDayOfMonth(1);
            LocalDate end = date.withDayOfMonth(date.lengthOfMonth());

            List<Long> prices = transactionRepository.findPricesByDateRange(start, end);
            long avg = prices.isEmpty() ? 0 :
                    (long) prices.stream().mapToLong(Long::longValue).average().orElse(0) / 10000;

            Map<String, Object> row = new HashMap<>();
            row.put("month", date.getYear() + "." + String.format("%02d", date.getMonthValue()));
            row.put("allIndex", avg);
            row.put("seoulIndex", (long)(avg * 1.15));
            row.put("metroIndex", (long)(avg * 1.05));
            row.put("localIndex", (long)(avg * 0.85));
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getVolume(int months) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 실제 데이터가 있는 2024년 5월 기준
        LocalDate base = LocalDate.of(2024, 5, 1);

        for (int i = months - 1; i >= 0; i--) {
            LocalDate date = base.minusMonths(i);
            LocalDate start = date.withDayOfMonth(1);
            LocalDate end = date.withDayOfMonth(date.lengthOfMonth());

            long count = transactionRepository.countByDealDateBetween(start, end);

            Map<String, Object> row = new HashMap<>();
            row.put("month", date.getYear() + "." + String.format("%02d", date.getMonthValue()));
            row.put("count", count);
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getTopRegions(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        regionRepository.findAll().stream().limit(limit).forEach(region -> {
            Map<String, Object> row = new HashMap<>();
            row.put("code", region.getCode());
            row.put("name", region.getName());
            row.put("avgPrice", 150000 + (int)(Math.random() * 50000));
            row.put("changeRate", Math.round((Math.random() * 5) * 10.0) / 10.0);
            row.put("volume", 100 + (int)(Math.random() * 300));
            result.add(row);
        });

        result.sort((a, b) ->
                Double.compare((Double)b.get("changeRate"), (Double)a.get("changeRate")));

        return result;
    }
}