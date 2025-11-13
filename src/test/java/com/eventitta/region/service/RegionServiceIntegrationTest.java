package com.eventitta.region.service;

import com.eventitta.region.domain.Region;
import com.eventitta.region.dto.response.RegionOptionResponse;
import com.eventitta.region.dto.response.RegionResponse;
import com.eventitta.region.repository.RegionRepository;
import com.eventitta.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegionServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RegionService regionService;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private RegionCacheService regionCacheService;

    @BeforeEach
    void clearRegionCaches() {
        regionCacheService.evictAllCaches();
    }

    @Test
    @DisplayName("실제 데이터로 계층 조회 테스트")
    void getRegionHierarchy_withRealData() {
        // Given: 테스트 데이터 생성
        Region seoul = new Region("1100000000", "서울특별시", null, 1);
        Region jongno = new Region("1100100000", "종로구", "1100000000", 2);
        Region cheongun = new Region("1100110100", "청운효자동", "1100100000", 3);

        regionRepository.save(seoul);
        regionRepository.save(jongno);
        regionRepository.save(cheongun);

        // When
        List<RegionResponse> hierarchy = regionService.getRegionHierarchy("1100110100");

        // Then
        assertEquals(3, hierarchy.size());
        assertEquals("서울특별시", hierarchy.get(0).name());
        assertEquals("종로구", hierarchy.get(1).name());
        assertEquals("청운효자동", hierarchy.get(2).name());
    }

    @Test
    @DisplayName("전체 지역 옵션 조회 - 성능 테스트")
    void getRegionOptions_performanceTest() {
        // Given: 테스트 데이터 생성
        Region seoul = new Region("1100000000", "서울특별시", null, 1);
        Region jongno = new Region("1100100000", "종로구", "1100000000", 2);
        Region cheongun = new Region("1100110100", "청운효자동", "1100100000", 3);
        Region gangnam = new Region("1100200000", "강남구", "1100000000", 2);
        Region samsung = new Region("1100210100", "삼성동", "1100200000", 3);

        regionRepository.saveAll(List.of(seoul, jongno, cheongun, gangnam, samsung));

        // When
        long start = System.currentTimeMillis();
        List<RegionOptionResponse> options = regionService.getRegionOptions();
        long duration = System.currentTimeMillis() - start;

        // Then
        assertTrue(duration < 200, "Should complete within 200ms, but took " + duration + "ms");
        assertEquals(2, options.size()); // cheongun, samsung만 리프 노드

        // 첫 번째 옵션 검증
        RegionOptionResponse first = options.stream()
            .filter(o -> o.code().equals("1100110100"))
            .findFirst()
            .orElse(null);

        assertNotNull(first);
        assertEquals("서울특별시 > 종로구 > 청운효자동", first.displayName());
        assertEquals(List.of("1100000000", "1100100000", "1100110100"), first.pathCodes());
        assertEquals(List.of("서울특별시", "종로구", "청운효자동"), first.pathNames());
    }

    @Test
    @DisplayName("캐시 적용 확인 - 두 번째 호출이 더 빠름")
    void getAllRegionsAsMap_cacheWorks() {
        // Given: 테스트 데이터 생성
        for (int i = 0; i < 100; i++) {
            String code = String.format("%10d", 1100000000L + i * 1000);
            regionRepository.save(new Region(code, "Region" + i, null, 1));
        }

        // First call (cache miss)
        long start1 = System.currentTimeMillis();
        Map<String, Region> regions1 = regionCacheService.getAllRegionsAsMap();
        long duration1 = System.currentTimeMillis() - start1;

        // Second call (cache hit)
        long start2 = System.currentTimeMillis();
        Map<String, Region> regions2 = regionCacheService.getAllRegionsAsMap();
        long duration2 = System.currentTimeMillis() - start2;

        // Then
        assertTrue(duration1 > duration2,
            "Cache should make second call faster. First: " + duration1 + "ms, Second: " + duration2 + "ms");
        assertEquals(regions1.size(), regions2.size());
        assertEquals(100, regions1.size());
    }

    @Test
    @DisplayName("캐시 무효화 테스트")
    void evictRegionCache_clearsCache() {
        // Given
        Region seoul = new Region("1100000000", "서울특별시", null, 1);
        regionRepository.save(seoul);

        // 캐시에 데이터 로드
        Map<String, Region> initial = regionCacheService.getAllRegionsAsMap();
        assertEquals(1, initial.size());

        // When - 캐시 무효화
        regionCacheService.evictAllCaches();

        // 새 데이터 추가
        Region busan = new Region("2600000000", "부산광역시", null, 1);
        regionRepository.save(busan);

        // Then - 캐시가 무효화되어 새 데이터가 보여야 함
        Map<String, Region> afterEvict = regionCacheService.getAllRegionsAsMap();
        assertEquals(2, afterEvict.size());
        assertNotNull(afterEvict.get("2600000000"));
    }
}
