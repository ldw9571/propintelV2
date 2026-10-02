package com.propintel.domain.complex.repository;

import com.propintel.domain.complex.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ⬇️ 추가
    List<Transaction> findByComplexId(Long complexId);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.complex.id = :complexId
          AND t.type = com.propintel.domain.complex.entity.Transaction$TransactionType.SALE
          AND t.dealDate BETWEEN :from AND :to
        ORDER BY t.dealDate
    """)
    List<Transaction> findSaleHistory(@Param("complexId") Long complexId,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    @Query(value = """
        SELECT t FROM Transaction t
        WHERE t.complex.id = :complexId
        ORDER BY t.dealDate DESC
        LIMIT :months
    """)
    List<Transaction> findRecentByComplex(@Param("complexId") Long complexId,
                                          @Param("months") int months);

    @Query("""
        SELECT YEAR(t.dealDate) as year,
               MONTH(t.dealDate) as month,
               COUNT(t) as cnt
        FROM Transaction t
        WHERE t.type = com.propintel.domain.complex.entity.Transaction$TransactionType.SALE
        GROUP BY YEAR(t.dealDate), MONTH(t.dealDate)
        ORDER BY year, month
    """)
    List<Object[]> findMonthlyVolume();

    @Query("SELECT t.price FROM Transaction t")
    List<Long> findAllPrices();

    @Query("SELECT t.price FROM Transaction t WHERE t.dealDate BETWEEN :start AND :end")
    List<Long> findPricesByDateRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    long countByDealDateBetween(LocalDate start, LocalDate end);
}