package com.propintel.infra.news;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 네이버 검색 API(뉴스) — https://developers.naver.com/docs/serviceapi/search/news/news.md
 * 제목·요약문(description)·원문 링크·게시일만 제공되며, 기사 본문은 제공되지 않는다.
 */
public class NaverNewsProvider implements NewsProvider {

    private static final DateTimeFormatter RFC = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final WebClient webClient;

    public NaverNewsProvider(WebClient.Builder builder, String clientId, String clientSecret) {
        this.webClient = builder.clone()
                .baseUrl("https://openapi.naver.com")
                .defaultHeader("X-Naver-Client-Id", clientId)
                .defaultHeader("X-Naver-Client-Secret", clientSecret)
                .build();
    }

    @Override public String name() { return "네이버 뉴스 검색 API"; }
    @Override public boolean isMock() { return false; }

    @Override
    public List<RawNews> search(String query, int display) {
        JsonNode res = webClient.get()
                .uri(u -> u.path("/v1/search/news.json").queryParam("query", query)
                        .queryParam("display", Math.min(display, 100)).queryParam("sort", "date").build())
                .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(15));
        List<RawNews> out = new ArrayList<>();
        if (res == null) return out;
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
