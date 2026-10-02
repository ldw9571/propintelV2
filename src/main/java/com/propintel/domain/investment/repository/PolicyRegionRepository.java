package com.propintel.domain.investment.repository;

import com.propintel.domain.investment.entity.PolicyRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyRegionRepository extends JpaRepository<PolicyRegion, Long> {
    Optional<PolicyRegion> findByCode(String code);
    List<PolicyRegion> findAllByOrderByIdAsc();
}
