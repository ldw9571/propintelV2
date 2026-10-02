package com.propintel.domain.complex.controller;

import com.propintel.domain.complex.service.ComplexService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/complexes")
@RequiredArgsConstructor
public class ComplexController {

    private final ComplexService complexService;

    // 단지 검색 (지역코드 + 이름)
    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(complexService.search(regionCode, name, pageable));
    }

    // 단지 상세
    @GetMapping("/{id}")
    public ResponseEntity<?> getComplex(@PathVariable Long id) {
        return ResponseEntity.ok(complexService.findById(id));
    }

    // 실거래가 이력
    @GetMapping("/{id}/transactions")
    public ResponseEntity<?> getSaleHistory(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(complexService.getSaleHistory(id, from, to));
    }

    // 유사 단지
    @GetMapping("/{id}/similar")
    public ResponseEntity<?> getSimilar(@PathVariable Long id) {
        return ResponseEntity.ok(complexService.findSimilar(id));
    }
}