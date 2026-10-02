package com.propintel.domain.score.entity;

import com.propintel.domain.complex.entity.Complex;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "complex_score")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComplexScore {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", nullable = false)
    private Complex complex;

    private Integer supplyScore;
    private Integer demandScore;
    private Integer transportScore;
    private Integer schoolScore;
    private Integer populationScore;
    private Integer totalScore;

    @Column(nullable = false)
    private LocalDate scoredAt;

    @Builder
    public ComplexScore(Complex complex, Integer supplyScore,
                        Integer demandScore, Integer transportScore,
                        Integer schoolScore, Integer populationScore,
                        Integer totalScore, LocalDate scoredAt) {
        this.complex = complex;
        this.supplyScore = supplyScore; this.demandScore = demandScore;
        this.transportScore = transportScore; this.schoolScore = schoolScore;
        this.populationScore = populationScore; this.totalScore = totalScore;
        this.scoredAt = scoredAt;
    }
}