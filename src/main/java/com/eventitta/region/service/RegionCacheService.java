package com.eventitta.region.service;

import com.eventitta.region.domain.Region;
import com.eventitta.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

import static com.eventitta.common.config.cache.CacheConstants.REGIONS;
import static com.eventitta.common.config.cache.CacheConstants.REGION_OPTIONS;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionCacheService {

    private final RegionRepository regionRepository;

    @Cacheable(value = REGIONS, key = "'allRegionsMap'")
    public Map<String, Region> getAllRegionsAsMap() {
        log.debug("캐시 미스 - DB에서 전체 지역 데이터 로드");
        return regionRepository.findAll()
            .stream()
            .collect(Collectors.toMap(Region::getCode, region -> region));
    }

    @CacheEvict(value = {REGIONS, REGION_OPTIONS}, allEntries = true)
    public void evictAllCaches() {
        log.info("지역 캐시 무효화 완료 - 다음 호출 시 DB에서 재로드됩니다");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        log.info("지역 캐시 워밍업 시작...");
        long startTime = System.currentTimeMillis();

        Map<String, Region> regionMap = getAllRegionsAsMap();

        long duration = System.currentTimeMillis() - startTime;
        log.info("지역 캐시 워밍업 완료 - {}개 지역 로드 ({}ms)", regionMap.size(), duration);
    }
}
