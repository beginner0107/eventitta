package com.eventitta.domain.region.service;

import com.eventitta.domain.region.api.internal.facade.RegionInternalFacade;
import com.eventitta.domain.region.api.internal.view.RegionReferenceView;
import com.eventitta.domain.region.domain.Region;
import com.eventitta.domain.region.exception.RegionErrorCode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class DefaultRegionInternalFacade implements RegionInternalFacade {

    private final RegionCacheService regionCacheService;

    @Override
    @Transactional(readOnly = true)
    public void ensureExists(String regionCode) {
        if (findByCode(regionCode).isEmpty()) {
            throw RegionErrorCode.NOT_FOUND_REGION_CODE.defaultException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegionReferenceView> findByCode(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return Optional.empty();
        }
        Region region = regionCacheService.getAllRegionsAsMap().get(regionCode);
        return Optional.ofNullable(region).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, RegionReferenceView> findByCodes(Collection<String> regionCodes) {
        if (regionCodes == null || regionCodes.isEmpty()) {
            return Map.of();
        }
        Map<String, Region> regionMap = regionCacheService.getAllRegionsAsMap();
        Map<String, RegionReferenceView> results = new LinkedHashMap<>();
        for (String regionCode : regionCodes) {
            Region region = regionMap.get(regionCode);
            if (region != null) {
                results.put(regionCode, toView(region));
            }
        }
        return results;
    }

    private RegionReferenceView toView(Region region) {
        return new RegionReferenceView(
            region.getCode(),
            region.getName(),
            region.getParentCode(),
            region.getLevel()
        );
    }
}
