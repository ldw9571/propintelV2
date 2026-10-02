package com.propintel.domain.report.entity;

import com.propintel.domain.complex.entity.Complex;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "ai_report")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", nullable = false)
    private Complex complex;

    @Column(columnDefinition = "TEXT") private String advantages;
    @Column(columnDefinition = "TEXT") private String risks;
    @Column(columnDefinition = "TEXT") private String checkpoints;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder
    public AiReport(Complex complex, String advantages,
                    String risks, String checkpoints) {
        this.complex = complex; this.advantages = advantages;
        this.risks = risks; this.checkpoints = checkpoints;
    }
}