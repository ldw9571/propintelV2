package com.propintel.domain.investment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import com.propintel.domain.investment.entity.PolicyParameter;

/** 기준일(asOf)에 유효한 행만 조회 (최신 effective_from 우선) */
public interface PolicyParameterRepository extends JpaRepository<PolicyParameter, Long> {
    @Query("select r from PolicyParameter r where r.effectiveFrom <= :d and (r.effectiveTo is null or r.effectiveTo > :d) order by r.effectiveFrom desc")
    List<PolicyParameter> findEffective(@Param("d") LocalDate asOf);
}
