package com.propintel.domain.watch.repository;

import com.propintel.domain.watch.entity.Alert;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    boolean existsByDedupeKey(String dedupeKey);

    @Query("select a from Alert a join fetch a.watchItem order by a.triggeredAt desc")
    List<Alert> findRecent(Pageable pageable);

    long countByReadFalse();

    void deleteByWatchItemId(Long watchItemId);
}
