package com.propintel.domain.complex.controller;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.complex.entity.Transaction;
import com.propintel.domain.complex.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;

    // 전체 거래 목록 (페이징)
    @GetMapping
    public ApiResponse<Page<Transaction>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.ok(transactionRepository.findAll(pageable));
    }

    // 특정 단지의 거래 내역
    @GetMapping("/complex/{complexId}")
    public ApiResponse<?> getByComplex(@PathVariable Long complexId) {
        return ApiResponse.ok(transactionRepository.findByComplexId(complexId));
    }
}