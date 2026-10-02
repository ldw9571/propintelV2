package com.propintel.domain.news.controller;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.news.entity.NewsCategory;
import com.propintel.domain.news.service.NewsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Phase 4: 부동산 뉴스 */
@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ApiResponse<List<NewsService.NewsView>> list(@RequestParam(required = false) String regionCode,
                                                        @RequestParam(required = false) NewsCategory category,
                                                        @RequestParam(defaultValue = "7") int days,
                                                        @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(newsService.list(regionCode, category, days, limit));
    }

    /** 지역 뉴스 즉시 수집 */
    @PostMapping("/collect/region/{code}")
    public ApiResponse<NewsService.CollectSummary> collectRegion(@PathVariable String code) {
        return ApiResponse.ok(newsService.collectRegion(code));
    }

    /** 정책·금리·공급 뉴스 즉시 수집 */
    @PostMapping("/collect/national")
    public ApiResponse<NewsService.CollectSummary> collectNational() {
        return ApiResponse.ok(newsService.collectNational());
    }
}
