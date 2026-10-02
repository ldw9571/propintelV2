package com.propintel.infra.openai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiClient {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .build())
            .build();

    public String chat(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            Map<?, ?> response = webClient.post()
                    .uri("/v1beta/models/gemini-pro:generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)  // ← 헤더 추가
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> first = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) first.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> part = (Map<?, ?>) parts.get(0);
            return (String) part.get("text");

        } catch (Exception e) {
            log.error("Gemini API 오류: {}", e.getMessage());
            throw new RuntimeException("Gemini 응답 오류: " + e.getMessage());
        }
    }
}