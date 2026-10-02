package com.propintel.domain.score.repository;

import com.propintel.domain.score.entity.ComplexScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplexScoreRepository extends JpaRepository<ComplexScore, Long> {
    Optional<ComplexScore> findTopByComplexIdOrderByScoredAtDesc(Long complexId);
    void deleteByComplexId(Long complexId);
}