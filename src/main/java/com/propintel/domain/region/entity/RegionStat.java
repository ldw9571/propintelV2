package com.propintel.domain.region.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "region_stat",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"region_id","stat_year","stat_month"}))
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionStat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    private Integer statYear;
    private Integer statMonth;
    private Integer population;
    private Integer jobs;
    private Double  priceIndex;
    private Double  jeonseRate;
    private Integer tradeVolume;

    @Builder
    public RegionStat(Region region, Integer statYear, Integer statMonth,
                      Integer population, Integer jobs, Double priceIndex,
                      Double jeonseRate, Integer tradeVolume) {
        this.region = region; this.statYear = statYear;
        this.statMonth = statMonth; this.population = population;
        this.jobs = jobs; this.priceIndex = priceIndex;
        this.jeonseRate = jeonseRate; this.tradeVolume = tradeVolume;
    }
}