package com.propintel.domain.region.repository;

import com.propintel.domain.region.entity.Region;
import com.propintel.domain.region.entity.RegionStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RegionStatRepository extends JpaRepository<RegionStat, Long> {

    List<RegionStat> findByRegionOrderByStatYearDescStatMonthDesc(Region region);

    List<RegionStat> findTop24ByRegionOrderByStatYearDescStatMonthDesc(Region region);

    @Query("""
        SELECT s FROM RegionStat s
        WHERE s.region.code = :code
          AND (s.statYear > :fromYear
               OR (s.statYear = :fromYear AND s.statMonth >= :fromMonth))
        ORDER BY s.statYear, s.statMonth
    """)
    List<RegionStat> findByRegionCodeFrom(@Param("code") String code,
                                          @Param("fromYear") int fromYear,
                                          @Param("fromMonth") int fromMonth);
}