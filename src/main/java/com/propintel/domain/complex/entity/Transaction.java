package com.propintel.domain.complex.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "transaction")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", nullable = false)
    private Complex complex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    private Integer  areaSqm;
    private Integer  floor;
    private Long     price;
    private Long     deposit;
    private Long     monthly;

    @Column(nullable = false)
    private LocalDate dealDate;

    public enum TransactionType { SALE, JEONSE, MONTHLY }

    @Builder
    public Transaction(Complex complex, TransactionType type,
                       Integer areaSqm, Integer floor, Long price,
                       Long deposit, Long monthly, LocalDate dealDate) {
        this.complex = complex; this.type = type;
        this.areaSqm = areaSqm; this.floor = floor;
        this.price = price; this.deposit = deposit;
        this.monthly = monthly; this.dealDate = dealDate;
    }
}