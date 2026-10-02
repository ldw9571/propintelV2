package com.propintel.domain.report.repository;

import com.propintel.domain.report.entity.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiReportRepository extends JpaRepository<AiReport, Long> {
    Optional<AiReport> findTopByComplexIdOrderByCreatedAtDesc(Long complexId);
    void deleteByComplexId(Long complexId);
}