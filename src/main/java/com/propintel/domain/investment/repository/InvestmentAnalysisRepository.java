package com.propintel.domain.investment.repository;

import com.propintel.domain.investment.entity.InvestmentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentAnalysisRepository extends JpaRepository<InvestmentAnalysis, Long> {
    List<InvestmentAnalysis> findAllByOrderByCreatedAtDesc();
}
