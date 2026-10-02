package com.propintel.domain.watch.repository;

import com.propintel.domain.watch.entity.WatchItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchItemRepository extends JpaRepository<WatchItem, Long> {
    List<WatchItem> findAllByOrderByCreatedAtAsc();
    List<WatchItem> findByActiveTrueOrderByCreatedAtAsc();
}
