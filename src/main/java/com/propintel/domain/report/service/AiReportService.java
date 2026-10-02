package com.propintel.domain.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propintel.domain.complex.entity.Complex;
import com.propintel.domain.complex.repository.ComplexRepository;
import com.propintel.domain.report.entity.AiReport;
import com.propintel.domain.report.repository.AiReportRepository;
import com.propintel.domain.score.entity.ComplexScore;
import com.propintel.domain.score.repository.ComplexScoreRepository;
import com.propintel.infra.openai.GeminiClient ;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiReportService {

    private final GeminiClient            openAiClient;
    private final ComplexRepository      complexRepository;
    private final ComplexScoreRepository scoreRepository;
    private final AiReportRepository     reportRepository;
    private final ObjectMapper           objectMapper;

    @Transactional
    public AiReport generateReport(Long complexId) {
        Complex complex = complexRepository.findById(complexId)
                .orElseThrow(() -> new EntityNotFoundException("단지 없음: " + complexId));

        ComplexScore score = scoreRepository
                .findTopByComplexIdOrderByScoredAtDesc(complexId)
                .orElseThrow(() -> new IllegalStateException("투자 점수를 먼저 계산해 주세요."));

        String raw = openAiClient.chat(buildPrompt(complex, score));
        ReportParseDto dto = parseGptResponse(raw);

        reportRepository.deleteByComplexId(complexId);

        return reportRepository.save(AiReport.builder()
                .complex(complex)
                .advantages(dto.advantages())
                .risks(dto.risks())
                .checkpoints(dto.checkpoints())
                .build());
    }

    public Optional<AiReport> getLatest(Long complexId) {
        return reportRepository.findTopByComplexIdOrderByCreatedAtDesc(complexId);
    }

    // ── 프롬프트 생성 ───────────────────────────────────────

    private String buildPrompt(Complex complex, ComplexScore score) {
        return """
            당신은 15년 경력의 한국 부동산 투자 전문가입니다.
            아래 데이터를 분석하여 투자 리포트를 JSON으로만 응답하세요.

            [단지 정보]
            - 이름: %s
            - 주소: %s
            - 건축연도: %d년 / 세대수: %d세대 / 층수: %d층

            [투자 점수 (100점 만점)]
            - 공급 점수: %d
            - 수요 점수: %d
            - 교통 점수: %d
            - 학군 점수: %d
            - 인구 점수: %d
            - 종합 점수: %d

            [응답 형식] JSON만 반환, 다른 텍스트 없음:
            {
              "advantages": "투자 장점 3가지 (줄바꿈 구분)",
              "risks": "투자 위험요소 3가지 (줄바꿈 구분)",
              "checkpoints": "향후 3~5년 체크포인트 3가지 (줄바꿈 구분)"
            }
            """.formatted(
                complex.getName(), complex.getAddress(),
                nullSafe(complex.getBuildYear()),
                nullSafe(complex.getTotalUnits()),
                nullSafe(complex.getFloorCount()),
                score.getSupplyScore(), score.getDemandScore(),
                score.getTransportScore(), score.getSchoolScore(),
                score.getPopulationScore(), score.getTotalScore()
        );
    }

    private ReportParseDto parseGptResponse(String json) {
        try {
            // GPT가 ```json ... ``` 블록으로 감싸는 경우 제거
            String cleaned = json.replaceAll("```json|```", "").trim();
            return objectMapper.readValue(cleaned, ReportParseDto.class);
        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패: {}", json, e);
            return new ReportParseDto("분석 준비 중", "분석 준비 중", "분석 준비 중");
        }
    }

    private int nullSafe(Integer val) {
        return val == null ? 0 : val;
    }

    public record ReportParseDto(
            String advantages,
            String risks,
            String checkpoints
    ) {}
}