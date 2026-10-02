package com.propintel.domain.watch.controller;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.watch.entity.WatchItem;
import com.propintel.domain.watch.service.MonitoringService;
import com.propintel.domain.watch.service.WatchDtos.*;
import com.propintel.domain.watch.service.WatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Phase 3: 관심지역 등록 → 가격 변화 모니터링 → 급등/급락 감지 → 알림 */
@RestController
@RequestMapping("/api/v1")
public class WatchController {

    private final WatchService watchService;
    private final MonitoringService monitoringService;

    public WatchController(WatchService watchService, MonitoringService monitoringService) {
        this.watchService = watchService;
        this.monitoringService = monitoringService;
    }

    @GetMapping("/watch")
    public ApiResponse<List<WatchStatus>> list() {
        return ApiResponse.ok(watchService.statuses());
    }

    @PostMapping("/watch")
    public ApiResponse<WatchStatus> create(@Valid @RequestBody CreateRequest request) {
        WatchItem w = watchService.create(request);
        return ApiResponse.ok(watchService.status(w.getId()));
    }

    @GetMapping("/watch/{id}")
    public ApiResponse<WatchStatus> get(@PathVariable Long id) {
        return ApiResponse.ok(watchService.status(id));
    }

    @PutMapping("/watch/{id}")
    public ApiResponse<WatchStatus> update(@PathVariable Long id, @RequestBody UpdateRequest request) {
        watchService.update(id, request);
        return ApiResponse.ok(watchService.status(id));
    }

    @DeleteMapping("/watch/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        watchService.delete(id);
        return ApiResponse.ok("삭제되었습니다.");
    }

    /** 알림 조건 조회/수정 (사용자가 기준값·사용 여부 설정) */
    @GetMapping("/watch/{id}/rules")
    public ApiResponse<List<RuleView>> rules(@PathVariable Long id) {
        return ApiResponse.ok(watchService.rules(id));
    }

    @PutMapping("/watch/{id}/rules")
    public ApiResponse<List<RuleView>> updateRules(@PathVariable Long id, @Valid @RequestBody List<RuleUpdate> updates) {
        return ApiResponse.ok(watchService.updateRules(id, updates));
    }

    /** 지금 바로 모든 관심 항목 평가 (스케줄러와 같은 동작) */
    @PostMapping("/watch/evaluate")
    public ApiResponse<EvaluateResult> evaluate() {
        return ApiResponse.ok(monitoringService.evaluateAll());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertView>> alerts(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(watchService.alerts(limit));
    }

    @GetMapping("/alerts/unread-count")
    public ApiResponse<Map<String, Long>> unread() {
        return ApiResponse.ok(Map.of("unread", watchService.unreadCount()));
    }

    @PostMapping("/alerts/{id}/read")
    public ApiResponse<String> read(@PathVariable Long id) {
        watchService.markRead(id);
        return ApiResponse.ok("읽음");
    }

    @PostMapping("/alerts/read-all")
    public ApiResponse<Map<String, Integer>> readAll() {
        return ApiResponse.ok(Map.of("updated", watchService.markAllRead()));
    }
}
