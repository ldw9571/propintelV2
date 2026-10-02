package com.propintel.domain.region.controller;

import com.propintel.domain.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    // 전체 지역 목록
    @GetMapping
    public ResponseEntity<?> getAllRegions() {
        return ResponseEntity.ok(regionService.findAll());
    }

    // 지역 코드로 조회
    @GetMapping("/{code}")
    public ResponseEntity<?> getRegion(@PathVariable String code) {
        return ResponseEntity.ok(regionService.findByCode(code));
    }

    // 최근 24개월 통계
    @GetMapping("/{code}/stats")
    public ResponseEntity<?> getRecentStats(@PathVariable String code) {
        return ResponseEntity.ok(regionService.getRecentStats(code));
    }

    // 특정 기간 통계
    @GetMapping("/{code}/stats/range")
    public ResponseEntity<?> getStats(
            @PathVariable String code,
            @RequestParam int fromYear,
            @RequestParam int fromMonth) {
        return ResponseEntity.ok(regionService.getStats(code, fromYear, fromMonth));
    }
}