package com.propintel.domain.market.repository;

import com.propintel.domain.complex.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 기존 transaction 테이블(매매)을 시장 통계용으로 읽는 추가 저장소 */
public interface SaleTradeRepository extends JpaRepository<Transaction, Long> {

    boolean existsByComplexIdAndTypeAndDealDateAndFloorAndAreaSqmAndPrice(
            Long complexId, Transaction.TransactionType type, LocalDate dealDate, Integer floor,
            Integer areaSqm, Long price);

    /** 나중에 해제(취소) 신고된 거래를 지운다. 지운 건수를 돌려준다 */
    @Transactional
    long deleteByComplexIdAndTypeAndDealDateAndFloorAndAreaSqmAndPrice(
            Long complexId, Transaction.TransactionType type, LocalDate dealDate, Integer floor,
            Integer areaSqm, Long price);

    @Query("""
        select t from Transaction t join fetch t.complex c join fetch c.region r
        where r.code = :code
          and t.type = com.propintel.domain.complex.entity.Transaction$TransactionType.SALE
          and t.price is not null
    """)
    List<Transaction> findSalesByRegion(@Param("code") String regionCode);

    @Query("""
        select t from Transaction t join fetch t.complex c
        where c.id = :complexId
          and t.type = com.propintel.domain.complex.entity.Transaction$TransactionType.SALE
          and t.price is not null
    """)
    List<Transaction> findSalesByComplex(@Param("complexId") Long complexId);
}
