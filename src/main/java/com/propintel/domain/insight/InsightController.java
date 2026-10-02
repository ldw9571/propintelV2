package com.propintel.domain.insight;

import com.propintel.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

/** Phase 5: 가격 변화 원인 분석 (근거 기반) */
@RestController
@RequestMapping("/api/v1/insights")
public class InsightController {

    private final PriceChangeInsightService service;

    public InsightController(PriceChangeInsightService service) {
        this.service = service;
    }

    /** 예: /api/v1/insights/price-change?regionCode=11560&months=3  또는  ?complexId=12&months=6 */
    @GetMapping("/price-change")
    public ApiResponse<PriceChangeInsightService.Insight> priceChange(@RequestParam(required = false) String regionCode,
                                                                      @RequestParam(required = false) Long complexId,
                                                                      @RequestParam(required = false) Double areaMin,
                                                                      @RequestParam(required = false) Double areaMax,
                                                                      @RequestParam(defaultValue = "3") int months,
                                                                      @RequestParam(defaultValue = "false") boolean ai) {
        if (regionCode == null && complexId == null) throw new IllegalArgumentException("regionCode 또는 complexId 가 필요합니다.");
        return ApiResponse.ok(service.analyze(regionCode, complexId, areaMin, areaMax, months, ai));
    }
}
