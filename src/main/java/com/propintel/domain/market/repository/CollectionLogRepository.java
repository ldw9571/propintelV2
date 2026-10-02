package com.propintel.domain.market.repository;

import com.propintel.domain.market.entity.CollectionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionLogRepository extends JpaRepository<CollectionLog, Long> {
    List<CollectionLog> findTop50ByOrderByCollectedAtDesc();
    Optional<CollectionLog> findTopByRegionCodeOrderByCollectedAtDesc(String regionCode);
}
