package com.propintel.domain.investment.repository;

import com.propintel.domain.investment.entity.InvestorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestorProfileRepository extends JpaRepository<InvestorProfile, Long> {
    Optional<InvestorProfile> findFirstByOrderByIdAsc();
}
