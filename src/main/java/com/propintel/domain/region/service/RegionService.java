package com.propintel.domain.region.service;

import com.propintel.domain.region.entity.Region;
import com.propintel.domain.region.entity.RegionStat;
import com.propintel.domain.region.repository.RegionRepository;
import com.propintel.domain.region.repository.RegionStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository     regionRepository;
    private final RegionStatRepository regionStatRepository;

    public List<Region> findAll() {
        return regionRepository.findAll();
    }

    public Region findByCode(String code) {
        return regionRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("지역 없음: " + code));
    }

    @Cacheable(value = "regionStats", key = "#code + '_' + #fromYear + '_' + #fromMonth")
    public List<RegionStat> getStats(String code, int fromYear, int fromMonth) {
        return regionStatRepository.findByRegionCodeFrom(code, fromYear, fromMonth);
    }

    @Cacheable(value = "regionStats", key = "#code + '_recent24'")
    public List<RegionStat> getRecentStats(String code) {
        Region region = findByCode(code);
        return regionStatRepository
                .findTop24ByRegionOrderByStatYearDescStatMonthDesc(region);
    }
}