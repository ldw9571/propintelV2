package com.propintel.infra.ai;

/**
 * 새 기능(뉴스 요약·원인 분석)에서 쓰는 LLM 추상화.
 * app.ai.provider = none | openai | gemini — 기존 OpenAiClient / GeminiClient 에 위임한다.
 * 비활성(none)이면 AI 문장을 만들지 않고 규칙 기반 결과만 보여준다.
 */
public interface AiTextClient {

    boolean isEnabled();

    String providerName();

    /** @return 모델 응답 텍스트. 비활성 또는 실패 시 null */
    String complete(String prompt);
}
