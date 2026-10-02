package com.propintel.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
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