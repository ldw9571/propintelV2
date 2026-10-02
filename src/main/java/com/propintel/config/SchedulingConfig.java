package com.propintel.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 관심지역 모니터링·뉴스 수집 스케줄러 활성화.
 * 기존 BatchScheduler(옛 매매 수집)는 app.legacy-batch.enabled=true 일 때만 동작한다 (기본 꺼짐).
 * 끄려면 app.scheduling.enabled=false
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
