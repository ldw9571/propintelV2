package com.propintel.domain.complex.service;

import com.propintel.domain.complex.entity.Complex;
import com.propintel.domain.complex.entity.Transaction;
import com.propintel.domain.complex.repository.ComplexRepository;
import com.propintel.domain.complex.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplexService {

    private final ComplexRepository     complexRepository;
    private final TransactionRepository transactionRepository;

    public Page<Complex> search(String regionCode, String name, Pageable pageable) {
        return complexRepository.searchByRegionAndName(regionCode, name, pageable);
    }

    public Complex findById(Long id) {
        return complexRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("단지 없음: " + id));
    }

    public List<Transaction> getSaleHistory(Long complexId, LocalDate from, LocalDate to) {
        return transactionRepository.findSaleHistory(complexId, from, to);
    }

    public List<Complex> findSimilar(Long complexId) {
        Complex c = findById(complexId);
        return complexRepository.findSimilar(
                c.getRegion().getCode(), complexId, c.getTotalUnits());
    }
}