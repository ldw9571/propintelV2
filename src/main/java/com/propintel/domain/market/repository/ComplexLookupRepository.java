package com.propintel.domain.market.repository;

import com.propintel.domain.complex.entity.Complex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 실거래 수집 시 단지 찾기 (중복 단지가 있어도 오류 없이 첫 번째를 사용) */
public interface ComplexLookupRepository extends JpaRepository<Complex, Long> {
    Optional<Complex> findFirstByNameAndAddressAndRegion_CodeOrderByIdAsc(String name, String address, String regionCode);
    Optional<Complex> findFirstByNameAndAddressOrderByIdAsc(String name, String address);
    List<Complex> findTop20ByRegion_CodeAndNameContainingOrderByNameAsc(String regionCode, String name);
}
