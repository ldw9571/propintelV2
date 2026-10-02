package com.propintel.batch;

import com.propintel.domain.complex.entity.Complex;
import com.propintel.domain.complex.entity.Transaction;
import com.propintel.domain.complex.repository.ComplexRepository;
import com.propintel.domain.complex.repository.TransactionRepository;
import com.propintel.infra.molit.MolitApiClient;
import com.propintel.infra.molit.dto.MolitTransactionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MolitTransactionJobConfig {

    private final JobRepository              jobRepository;
    private final PlatformTransactionManager txManager;
    private final MolitApiClient             molitApiClient;
    private final ComplexRepository          complexRepository;
    private final TransactionRepository      transactionRepository;

    @Bean
    public Job molitTransactionJob() {
        return new JobBuilder("molitTransactionJob", jobRepository)
                .start(molitTransactionStep())
                .build();
    }

    @Bean
    public Step molitTransactionStep() {
        return new StepBuilder("molitTransactionStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    YearMonth target = YearMonth.now().minusMonths(1);
                    String dealYmd = target.getYear()
                            + String.format("%02d", target.getMonthValue());

                    log.info("국토부 실거래가 수집 시작: {}", dealYmd);

                    // 전체 지역 순회 오버로드 사용
                    List<MolitTransactionDto> dtos =
                            molitApiClient.fetchTransactions(dealYmd);

                    int saved = 0;
                    for (MolitTransactionDto dto : dtos) {
                        Complex complex = complexRepository
                                .findByNameAndAddress(dto.aptName(), dto.umdNm())
                                .orElse(null);

                        if (complex == null) {
                            continue; // 등록 안 된 단지는 건너뜀
                        }

                        try {
                            transactionRepository.save(
                                    Transaction.builder()
                                            .complex(complex)
                                            .type(Transaction.TransactionType.SALE)
                                            .areaSqm((int) Double.parseDouble(dto.excluUseAr()))
                                            .floor(Integer.parseInt(dto.floor().trim()))
                                            .price(Long.parseLong(dto.dealAmount().replace(",", "").trim()) * 10000L)
                                            .dealDate(LocalDate.of(
                                                    Integer.parseInt(dto.dealYear()),
                                                    Integer.parseInt(dto.dealMonth()),
                                                    Integer.parseInt(dto.dealDay().trim())))
                                            .build()
                            );
                            saved++;
                        } catch (Exception e) {
                            log.warn("거래 저장 실패: {} - {}", dto.aptName(), e.getMessage());
                        }
                    }

                    log.info("국토부 수집 완료: {}건", saved);
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }
}