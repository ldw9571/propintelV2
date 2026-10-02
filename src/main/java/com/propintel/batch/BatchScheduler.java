package com.propintel.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * 기존(옛) 매매 수집 스케줄러. 기본으로 꺼 둔다.
 * - 옛 수집기는 해제 거래를 거르지 않고 중복 저장 방지도 없어, 새 수집기(TransactionIngestService)와 함께 돌면
 *   같은 거래가 두 번 저장되어 통계가 틀어진다.
 * - kosisStatJob 빈이 없어 runKosisJob 도 같은 매매 Job 을 한 번 더 실행한다.
 * 관심지역 실거래는 MonitoringScheduler 가 매일 새 수집기로 받는다. 꼭 필요할 때만 app.legacy-batch.enabled=true
 */
@Component
@ConditionalOnProperty(name = "app.legacy-batch.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job molitTransactionJob;
    private final Job kosisStatJob;

    // 매월 1일 오전 2시 — 국토부 실거래가
    @Scheduled(cron = "0 0 2 1 * *")
    public void runMolitJob() {
        runJob(molitTransactionJob, "molitTransactionJob");
    }

    // 매월 5일 오전 3시 — KOSIS 통계
    @Scheduled(cron = "0 0 3 5 * *")
    public void runKosisJob() {
        runJob(kosisStatJob, "kosisStatJob");
    }

    private void runJob(Job job, String jobName) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runAt", LocalDateTime.now().toString())
                    .toJobParameters();
            JobExecution exec = jobLauncher.run(job, params);
            log.info("{} 완료: status={}", jobName, exec.getStatus());
        } catch (Exception e) {
            log.error("{} 실행 오류", jobName, e);
        }
    }
}