package com.propintel.domain.home;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.investment.dto.InvestmentDtos.Capacity;
import com.propintel.domain.investment.service.CapacityService;
import com.propintel.domain.news.entity.NewsCategory;
import com.propintel.domain.news.service.NewsService;
import com.propintel.domain.news.service.NewsService.NewsView;
import com.propintel.domain.watch.entity.WatchItem;
import com.propintel.domain.watch.service.WatchDtos.AlertView;
import com.propintel.domain.watch.service.WatchDtos.WatchStatus;
import com.propintel.domain.watch.service.WatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/** 첫 화면: 나의 투자 가능 금액 · 관심 지역 · 관심 아파트 · 오늘의 뉴스 · 알림 */
@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    public record NewsGroups(List<NewsView> region, List<NewsView> policy, List<NewsView> rate, List<NewsView> supply,
                             boolean mockProvider) {}

    public record Home(Capacity capacity, String capacityError, List<WatchStatus> regions, List<WatchStatus> complexes,
                       NewsGroups news, List<AlertView> alerts, long unreadAlerts, List<String> errors) {}

    private final CapacityService capacityService;
    private final WatchService watchService;
    private final NewsService newsService;

    public HomeController(CapacityService capacityService, WatchService watchService, NewsService newsService) {
        this.capacityService = capacityService;
        this.watchService = watchService;
        this.newsService = newsService;
    }

    @GetMapping
    public ApiResponse<Home> home() {
        List<String> errors = new ArrayList<>();
        Capacity cap = null;
        String capErr = null;
        try {
            cap = capacityService.capacity();
        } catch (RuntimeException e) {
            capErr = e.getMessage();
        }
        List<WatchStatus> watches = List.of();
        try {
            watches = watchService.statuses();
        } catch (RuntimeException e) {
            log.warn("홈 관심목록 오류", e);
            errors.add("관심 목록: " + e.getMessage());
        }
        List<WatchStatus> regions = watches.stream().filter(w -> w.targetType() == WatchItem.TargetType.REGION).toList();
        List<WatchStatus> complexes = watches.stream().filter(w -> w.targetType() == WatchItem.TargetType.COMPLEX).toList();

        Set<String> codes = new LinkedHashSet<>();
        watches.forEach(w -> codes.add(w.regionCode()));
        List<NewsView> regionNews = new ArrayList<>();
        for (String c : codes) regionNews.addAll(newsService.list(c, null, 2, 5));
        regionNews.sort(Comparator.comparing(NewsView::publishedAt).reversed());
        NewsGroups news = new NewsGroups(regionNews.stream().limit(8).toList(),
                national(NewsCategory.POLICY), national(NewsCategory.RATE), national(NewsCategory.SUPPLY),
                newsService.isMockProvider());

        return ApiResponse.ok(new Home(cap, capErr, regions, complexes, news, watchService.alerts(10),
                watchService.unreadCount(), errors));
    }

    private List<NewsView> national(NewsCategory c) {
        return newsService.list(null, c, 3, 20).stream().filter(n -> n.regionCode() == null).limit(4).toList();
    }
}
