package com.propintel.domain.news.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.propintel.domain.news.entity.NewsArticle;
import com.propintel.infra.ai.AiTextClient;
import org.springframework.stereotype.Component;

/**
 * 뉴스 요약. AI 사용 시에도 입력은 "제목 + 검색 API 요약문"뿐이므로,
 * 그 텍스트에 없는 내용(수치·전망·추측)을 쓰지 않도록 지시하고,
 * 응답이 이상하면 원문 요약문을 그대로 쓴다.
 */
@Component
public class NewsSummarizer {

    private final AiTextClient ai;
    private final ObjectMapper objectMapper;

    public NewsSummarizer(AiTextClient ai, ObjectMapper objectMapper) {
        this.ai = ai;
        this.objectMapper = objectMapper;
    }

    public void summarize(NewsArticle a) {
        String extract = a.getDescription() == null || a.getDescription().isBlank() ? a.getTitle() : a.getDescription();
        String rule = NewsClassifier.impactNote(a.getCategory());
        if (!ai.isEnabled() || a.isMock()) {
            a.summarize(extract, rule, NewsArticle.SummaryMethod.EXTRACT);
            return;
        }
        String prompt = """
                아래는 뉴스 기사의 제목과 검색 서비스가 제공한 짧은 요약문입니다. 기사 본문은 없습니다.
                규칙:
                1) 아래 텍스트에 있는 내용만 사용하세요. 텍스트에 없는 수치, 전망, 원인, 추측을 추가하지 마세요.
                2) summary: 핵심 내용을 한국어 1~2문장으로.
                3) impact: 해당 지역 부동산에 영향을 줄 수 있는 내용이 텍스트에 "언급되어 있는지"만 쓰세요.
                   예) "기사에 ○○역 개통 일정이 언급됨". 언급이 없으면 "텍스트만으로는 확인할 수 없음".
                   가격이 오른다/내린다 같은 예측은 쓰지 마세요.
                4) JSON만 출력: {"summary": "...", "impact": "..."}

                [제목] %s
                [요약문] %s
                """.formatted(a.getTitle(), extract);
        String res = ai.complete(prompt);
        try {
            JsonNode n = objectMapper.readTree(res.replaceAll("```json|```", "").trim());
            String summary = n.path("summary").asText("").trim();
            String impact = n.path("impact").asText("").trim();
            if (summary.isEmpty() || summary.length() > 400) throw new IllegalStateException("요약 형식 오류");
            a.summarize(summary, impact.isEmpty() ? rule : impact, NewsArticle.SummaryMethod.AI);
        } catch (Exception e) {
            a.summarize(extract, rule, NewsArticle.SummaryMethod.EXTRACT);
        }
    }
}
