package com.propintel.domain.region.repository;

import com.propintel.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByCode(String code);

    @Query("""
        SELECT r FROM Region r
        WHERE (:city IS NULL OR r.city = :city)
        ORDER BY r.name
    """)
    List<Region> findAllByCity(@Param("city") String city);

    @Query("""
        SELECT r FROM Region r
        WHERE r.lat BETWEEN :swLat AND :neLat
          AND r.lng BETWEEN :swLng AND :neLng
    """)
    List<Region> findByBounds(@Param("swLat") double swLat,
                              @Param("neLat") double neLat,
                              @Param("swLng") double swLng,
                              @Param("neLng") double neLng);
}