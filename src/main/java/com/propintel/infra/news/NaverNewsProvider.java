package com.propintel.infra.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 네이버 검색 API(뉴스) — NAVER API HUB (네이버 클라우드 플랫폼)
 * 2026-07-31부터 개발자센터(openapi.naver.com) 신규 발급이 중단되어 HUB로 전환.
 * 이관 가이드: https://guide.ncloud-docs.com/docs/apihub-migration
 *  - 주소: https://naverapihub.apigw.ntruss.com/search/v1/news
 *  - 헤더: X-NCP-APIGW-API-KEY-ID / X-NCP-APIGW-API-KEY
 * HUB 응답은 Content-Type 이 JSON 으로 인식되지 않을 수 있어, 문자열로 받은 뒤 직접 파싱한다.
 * 제목·요약문(description)·원문 링크·게시일만 제공되며, 기사 본문은 제공되지 않는다.
 */
public class NaverNewsProvider implements NewsProvider {

    private static final Logger log = LoggerFactory.getLogger(NaverNewsProvider.class);
    private static final DateTimeFormatter RFC = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final String BASE_URL = "https://naverapihub.apigw.ntruss.com";
    static final String NEWS_PATH = "/search/v1/news";

    private final WebClient webClient;

    public NaverNewsProvider(WebClient.Builder builder, String clientId, String clientSecret) {
        this.webClient = builder.clone()
                .baseUrl(BASE_URL)
                .defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                .defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }

    @Override public String name() { return "네이버 뉴스 검색 API (API HUB)"; }
    @Override public boolean isMock() { return false; }

    @Override
    public List<RawNews> search(String query, int display) {
        String body = webClient.get()
                .uri(u -> u.path(NEWS_PATH).queryParam("query", query)
                        .queryParam("display", Math.min(display, 100)).queryParam("sort", "date").build())
                .retrieve().bodyToMono(String.class).block(Duration.ofSeconds(15));
        List<RawNews> out = new ArrayList<>();
        if (body == null || body.isBlank()) return out;

        JsonNode res;
        try {
            res = MAPPER.readTree(body);
        } catch (Exception e) {
            log.warn("[뉴스] '{}' 응답 JSON 파싱 실패: {}", query, body.length() > 300 ? body.substring(0, 300) : body);
            return out;
        }
        if (!res.has("items")) {
            log.warn("[뉴스] '{}' 응답에 items 없음: {}", query, body.length() > 300 ? body.substring(0, 300) : body);
            return out;
        }
        for (JsonNode n : res.path("items")) {
            LocalDateTime at;
            try {
                at = ZonedDateTime.parse(n.path("pubDate").asText(), RFC).toLocalDateTime();
            } catch (Exception e) {
                at = LocalDateTime.now();
            }
            out.add(new RawNews(clean(n.path("title").asText()), n.path("link").asText(),
                    n.path("originallink").asText(null), clean(n.path("description").asText()), at));
        }
        return out;
    }

    /** <b> 태그와 HTML 엔티티 제거 */
    static String clean(String s) {
        if (s == null) return null;
        return s.replaceAll("<[^>]+>", "")
                .replace("&quot;", "\"").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&apos;", "'").replace("&#39;", "'").trim();
    }
}
