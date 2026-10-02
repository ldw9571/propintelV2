package com.propintel.domain.market.repository;

import com.propintel.domain.market.entity.RentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentTransactionRepository extends JpaRepository<RentTransaction, Long> {

    boolean existsByComplexIdAndDealDateAndFloorAndAreaSqmAndDepositAndMonthlyRent(
            Long complexId, LocalDate dealDate, int floor, double areaSqm, long deposit, long monthlyRent);

    @Query("select r from RentTransaction r join fetch r.complex c where r.regionCode = :code and r.dealDate >= :from")
    List<RentTransaction> findByRegionFrom(@Param("code") String regionCode, @Param("from") LocalDate from);

    @Query("select r from RentTransaction r join fetch r.complex c where c.id = :complexId and r.dealDate >= :from")
    List<RentTransaction> findByComplexFrom(@Param("complexId") Long complexId, @Param("from") LocalDate from);
}
