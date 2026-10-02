package com.propintel.domain.complex.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.propintel.domain.region.entity.Region;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "complex")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // ← 이 줄 추가
public class Complex {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 500) private String address;
    private Integer buildYear;
    private Integer totalUnits;
    private Integer floorCount;
    private Double  lat;
    private Double  lng;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder
    public Complex(Region region, String name, String address,
                   Integer buildYear, Integer totalUnits,
                   Integer floorCount, Double lat, Double lng) {
        this.region = region; this.name = name; this.address = address;
        this.buildYear = buildYear; this.totalUnits = totalUnits;
        this.floorCount = floorCount; this.lat = lat; this.lng = lng;
    }
}