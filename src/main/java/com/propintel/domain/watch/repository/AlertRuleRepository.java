package com.propintel.domain.watch.repository;

import com.propintel.domain.watch.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByWatchItemIdOrderByIdAsc(Long watchItemId);
    void deleteByWatchItemId(Long watchItemId);
}
