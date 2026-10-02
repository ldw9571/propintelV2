package com.propintel.domain.news.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 수집한 뉴스 1건 (제목·언론사·날짜·원문 링크·요약·지역 영향 관련 언급) */
@Entity
@Table(name = "news_article", indexes = {
        @Index(name = "idx_news_region_date", columnList = "region_code, published_at"),
        @Index(name = "idx_news_category_date", columnList = "category, published_at")})
public class NewsArticle {

    public enum SummaryMethod { AI, EXTRACT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500) private String title;
    @Column(length = 120) private String publisher;
    @Column(nullable = false, unique = true, length = 1000) private String url;
    @Column(name = "original_url", length = 1000) private String originalUrl;
    @Column(name = "published_at", nullable = false) private LocalDateTime publishedAt;
    @Column(columnDefinition = "TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private NewsCategory category;
    @Column(length = 200) private String keyword;
    /** 관심지역 코드 (정책·금리 등 전국 뉴스는 null) */
    @Column(name = "region_code", length = 20) private String regionCode;
    /** 핵심 내용 요약 — 원문(제목·요약문)에 있는 내용만 */
    @Column(columnDefinition = "TEXT") private String summary;
    /** 해당 지역에 영향을 줄 수 있는 내용이 기사에 언급됐는지 (예측이 아니라 언급 사실) */
    @Column(name = "impact_note", columnDefinition = "TEXT") private String impactNote;
    @Enumerated(EnumType.STRING) @Column(name = "summary_method", length = 10) private SummaryMethod summaryMethod;
    @Column(name = "source_name", nullable = false, length = 100) private String sourceName;
    @Column(nullable = false) private boolean mock;
    @Column(name = "collected_at", nullable = false) private LocalDateTime collectedAt;

    protected NewsArticle() {}

    public NewsArticle(String title, String publisher, String url, String originalUrl, LocalDateTime publishedAt,
                       String description, NewsCategory category, String keyword, String regionCode,
                       String sourceName, boolean mock) {
        this.title = cut(title, 500);
        this.publisher = cut(publisher, 120);
        this.url = cut(url, 1000);
        this.originalUrl = cut(originalUrl, 1000);
        this.publishedAt = publishedAt;
        this.description = description;
        this.category = category;
        this.keyword = cut(keyword, 200);
        this.regionCode = regionCode;
        this.sourceName = sourceName;
        this.mock = mock;
        this.collectedAt = LocalDateTime.now();
    }

    public void summarize(String summary, String impactNote, SummaryMethod method) {
        this.summary = summary;
        this.impactNote = impactNote;
        this.summaryMethod = method;
    }

    private static String cut(String s, int n) {
        return s == null || s.length() <= n ? s : s.substring(0, n);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getPublisher() { return publisher; }
    public String getUrl() { return url; }
    public String getOriginalUrl() { return originalUrl; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public String getDescription() { return description; }
    public NewsCategory getCategory() { return category; }
    public String getKeyword() { return keyword; }
    public String getRegionCode() { return regionCode; }
    public String getSummary() { return summary; }
    public String getImpactNote() { return impactNote; }
    public SummaryMethod getSummaryMethod() { return summaryMethod; }
    public String getSourceName() { return sourceName; }
    public boolean isMock() { return mock; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
}
