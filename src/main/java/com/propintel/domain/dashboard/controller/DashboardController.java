package com.propintel.domain.dashboard.controller;

import com.propintel.common.dto.ApiResponse;
import com.propintel.domain.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ApiResponse<?> getSummary() {
        return ApiResponse.ok(dashboardService.getSummary());
    }

    @GetMapping("/price-index")
    public ApiResponse<?> getPriceIndex(
            @RequestParam(defaultValue = "ALL") String area,
            @RequestParam(defaultValue = "24") int months) {
        return ApiResponse.ok(dashboardService.getPriceIndex(area, months));
    }

    @GetMapping("/volume")
    public ApiResponse<?> getVolume(
            @RequestParam(defaultValue = "12") int months) {
        return ApiResponse.ok(dashboardService.getVolume(months));
    }

    @GetMapping("/top-regions")
    public ApiResponse<?> getTopRegions(
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(dashboardService.getTopRegions(limit));
    }
}