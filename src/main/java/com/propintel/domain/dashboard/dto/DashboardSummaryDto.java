package com.propintel.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardSummaryDto {
    private Long avgPrice;
    private Long totalVolume;
    private Double jeonseRatio;
    private Double monthlyChange;
}