package com.propintel.domain.region.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "region")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // ← 이 줄 추가
public class Region {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50) private String city;
    @Column(length = 50) private String district;
    @Column(length = 50) private String dong;

    private Double lat;
    private Double lng;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder
    public Region(String code, String name, String city,
                  String district, String dong, Double lat, Double lng) {
        this.code = code; this.name = name; this.city = city;
        this.district = district; this.dong = dong;
        this.lat = lat; this.lng = lng;
    }
}