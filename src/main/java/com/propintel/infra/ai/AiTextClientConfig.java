package com.propintel.infra.ai;

import com.propintel.infra.openai.GeminiClient;
import com.propintel.infra.openai.OpenAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiTextClientConfig {

    private static final Logger log = LoggerFactory.getLogger(AiTextClientConfig.class);

    @Bean
    public AiTextClient aiTextClient(@Value("${app.ai.provider:none}") String provider,
                                     OpenAiClient openAiClient, GeminiClient geminiClient) {
        String p = provider == null ? "none" : provider.trim().toLowerCase();
        log.info("[AI] 뉴스 요약·원인 분석 provider = {}", p);
        return switch (p) {
            case "openai" -> new Delegate("openai", openAiClient::chat);
            case "gemini" -> new Delegate("gemini", geminiClient::chat);
            default -> new Delegate("none", null);
        };
    }

    static final class Delegate implements AiTextClient {
        private final String name;
        private final java.util.function.Function<String, String> fn;

        Delegate(String name, java.util.function.Function<String, String> fn) {
            this.name = name;
            this.fn = fn;
        }

        @Override public boolean isEnabled() { return fn != null; }
        @Override public String providerName() { return name; }

        @Override
        public String complete(String prompt) {
            if (fn == null) return null;
            try {
                return fn.apply(prompt);
            } catch (RuntimeException e) {
                log.warn("[AI] 호출 실패({}): {}", name, e.getMessage());
                return null;
            }
        }
    }
}
