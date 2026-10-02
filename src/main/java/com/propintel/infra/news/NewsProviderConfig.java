package com.propintel.infra.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NewsProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(NewsProviderConfig.class);

    @Bean
    public NewsProvider newsProvider(WebClient.Builder builder,
                                     @Value("${naver.client-id:}") String clientId,
                                     @Value("${naver.client-secret:}") String clientSecret) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.info("[뉴스] NAVER_CLIENT_ID/SECRET 미설정 → 샘플(Mock) 뉴스 사용");
            return new MockNewsProvider();
        }
        log.info("[뉴스] 네이버 뉴스 검색 API 사용");
        return new NaverNewsProvider(builder, clientId, clientSecret);
    }
}
