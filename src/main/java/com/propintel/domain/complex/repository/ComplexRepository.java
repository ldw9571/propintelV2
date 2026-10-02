package com.propintel.domain.complex.repository;

import com.propintel.domain.complex.entity.Complex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplexRepository extends JpaRepository<Complex, Long> {

    Optional<Complex> findById(Long id);

    Optional<Complex> findByNameAndAddress(String name, String address);

    // ⬇️ 추가한 메서드
    List<Complex> findByRegionId(Long regionId);

    @Query("SELECT c FROM Complex c WHERE " +
            "(:regionCode IS NULL OR c.region.code = :regionCode) AND " +
            "(:name IS NULL OR c.name LIKE %:name%)")
    Page<Complex> searchByRegionAndName(
            @Param("regionCode") String regionCode,
            @Param("name") String name,
            Pageable pageable
    );

    @Query("SELECT c FROM Complex c WHERE c.region.code = :regionCode AND c.id != :excludeId AND c.buildYear BETWEEN :buildYear - 3 AND :buildYear + 3")
    List<Complex> findSimilar(
            @Param("regionCode") String regionCode,
            @Param("excludeId") Long excludeId,
            @Param("buildYear") Integer buildYear
    );
}