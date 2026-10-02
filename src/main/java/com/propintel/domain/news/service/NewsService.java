package com.propintel.domain.news.service;

import com.propintel.domain.investment.entity.PolicyRegion;
import com.propintel.domain.investment.service.PolicyService;
import com.propintel.domain.news.entity.NewsArticle;
import com.propintel.domain.news.entity.NewsCategory;
import com.propintel.domain.news.repository.NewsArticleRepository;
import com.propintel.infra.news.NewsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

/** Phase 4: 관심지역·정책·금리·공급 뉴스 수집과 조회 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    /** 관심지역 등록 시 함께 검색하는 키워드 */
    public static final List<String> REGION_TOPICS = List.of("부동산", "재개발", "재건축", "교통", "입주", "분양", "정비사업");

    /** 전국 단위 뉴스 키워드 (regionCode = null) */
    public static final Map<String, NewsCategory> NATIONAL_QUERIES = new LinkedHashMap<>();
    static {
        NATIONAL_QUERIES.put("부동산 대책", NewsCategory.POLICY);
        NATIONAL_QUERIES.put("주택담보대출 규제", NewsCategory.POLICY);
        NATIONAL_QUERIES.put("기준금리 한국은행", NewsCategory.RATE);
        NATIONAL_QUERIES.put("주담대 금리", NewsCategory.RATE);
        NATIONAL_QUERIES.put("주택 공급 대책", NewsCategory.SUPPLY);
        NATIONAL_QUERIES.put("아파트 입주 물량", NewsCategory.SUPPLY);
    }

    public record CollectSummary(String provider, boolean mock, int queries, int saved, int duplicates, List<String> errors) {}

    public record NewsView(Long id, String title, String publisher, String url, LocalDateTime publishedAt,
                           String category, String categoryLabel, String regionCode, String keyword,
                           String summary, String impactNote, String summaryMethod, String sourceName, boolean mock) {
        public static NewsView of(NewsArticle a) {
            return new NewsView(a.getId(), a.getTitle(), a.getPublisher(),
                    a.getOriginalUrl() != null && !a.getOriginalUrl().isBlank() ? a.getOriginalUrl() : a.getUrl(),
                    a.getPublishedAt(), a.getCategory().name(), a.getCategory().label(), a.getRegionCode(),
                    a.getKeyword(), a.getSummary(), a.getImpactNote(),
                    a.getSummaryMethod() == null ? null : a.getSummaryMethod().name(), a.getSourceName(), a.isMock());
        }
    }

    private final NewsProvider provider;
    private final NewsArticleRepository repository;
    private final NewsSummarizer summarizer;
    private final PolicyService policyService;

    public NewsService(NewsProvider provider, NewsArticleRepository repository, NewsSummarizer summarizer,
                       PolicyService policyService) {
        this.provider = provider;
        this.repository = repository;
        this.summarizer = summarizer;
        this.policyService = policyService;
    }

    /** 지역 뉴스 수집: "영등포구 부동산", "영등포구 재개발" ... */
    @Transactional
    public CollectSummary collectRegion(String regionCode) {
        PolicyRegion r = policyService.region(regionCode);
        String name = r.shortName();
        Map<String, String> queries = new LinkedHashMap<>();
        for (String topic : REGION_TOPICS) queries.put(name + " " + topic, regionCode);
        return collect(queries, null, 10);
    }

    /** 정책·금리·공급 등 전국 뉴스 수집 */
    @Transactional
    public CollectSummary collectNational() {
        Map<String, String> queries = new LinkedHashMap<>();
        NATIONAL_QUERIES.keySet().forEach(q -> queries.put(q, null));
        return collect(queries, NATIONAL_QUERIES, 10);
    }

    private CollectSummary collect(Map<String, String> queries, Map<String, NewsCategory> fixedCategory, int display) {
        int saved = 0, dup = 0;
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, String> q : queries.entrySet()) {
            try {
                for (NewsProvider.RawNews n : provider.search(q.getKey(), display)) {
                    if (n.link() == null || n.link().isBlank() || repository.existsByUrl(n.link())) { dup++; continue; }
                    boolean regional = q.getValue() != null;
                    NewsCategory cat = NewsClassifier.classify(n.title(), n.description(), regional);
                    if (!regional && fixedCategory != null && (cat == NewsCategory.OTHER)) cat = fixedCategory.get(q.getKey());
                    NewsArticle a = new NewsArticle(n.title(), publisher(n), n.link(), n.originalLink(), n.publishedAt(),
                            n.description(), cat, q.getKey(), q.getValue(), provider.name(), provider.isMock());
                    summarizer.summarize(a);
                    repository.save(a);
                    saved++;
                }
            } catch (RuntimeException e) {
                log.warn("[뉴스] '{}' 수집 실패: {}", q.getKey(), e.getMessage());
                errors.add(q.getKey() + ": " + e.getMessage());
            }
        }
        return new CollectSummary(provider.name(), provider.isMock(), queries.size(), saved, dup, errors);
    }

    @Transactional(readOnly = true)
    public List<NewsView> list(String regionCode, NewsCategory category, int days, int limit) {
        LocalDateTime from = LocalDateTime.now().minusDays(Math.max(1, days));
        return repository.search(blankToNull(regionCode), category, from, PageRequest.of(0, Math.min(limit, 200)))
                .stream().map(NewsView::of).toList();
    }

    /** 특정 기간의 지역 뉴스 (알림·원인 분석에서 "관련 뉴스"로 사용) */
    @Transactional(readOnly = true)
    public List<NewsView> regionNewsBetween(String regionCode, LocalDateTime from, LocalDateTime to) {
        return repository.findByRegionCodeAndPublishedAtBetweenOrderByPublishedAtDesc(regionCode, from, to)
                .stream().map(NewsView::of).toList();
    }

    @Transactional(readOnly = true)
    public List<NewsView> nationalNewsBetween(Collection<NewsCategory> cats, LocalDateTime from, LocalDateTime to) {
        return repository.findByRegionCodeIsNullAndCategoryInAndPublishedAtBetweenOrderByPublishedAtDesc(cats, from, to)
                .stream().map(NewsView::of).toList();
    }

    public boolean isMockProvider() {
        return provider.isMock();
    }

    /** 원문 링크의 도메인을 언론사 표시로 사용 (검색 API는 언론사명을 주지 않음) */
    static String publisher(NewsProvider.RawNews n) {
        String link = n.originalLink() != null && !n.originalLink().isBlank() ? n.originalLink() : n.link();
        try {
            String host = URI.create(link).getHost();
            return host == null ? null : host.replaceFirst("^www\\.", "").replaceFirst("^m\\.", "");
        } catch (Exception e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
