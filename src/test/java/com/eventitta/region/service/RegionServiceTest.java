package com.eventitta.region.service;

import com.eventitta.region.domain.Region;
import com.eventitta.region.dto.response.RegionOptionResponse;
import com.eventitta.region.dto.response.RegionResponse;
import com.eventitta.region.exception.RegionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    private RegionCacheService cacheService;
    private RegionService regionService;

    @BeforeEach
    void setUp() {
        cacheService = mock(RegionCacheService.class);
        regionService = new RegionService(cacheService);
    }

    @Test
    void getTopLevelRegions_returnsDtoList() {
        Region r1 = new Region("1100000000", "서울특별시", null, 1);
        Region r2 = new Region("2600000000", "부산광역시", null, 1);
        Map<String, Region> map = Map.of(
            r1.getCode(), r1,
            r2.getCode(), r2
        );
        when(cacheService.getAllRegionsAsMap()).thenReturn(map);

        List<RegionResponse> result = regionService.getTopLevelRegions();

        assertEquals(2, result.size());
        Set<String> codes = new HashSet<>(result.stream().map(RegionResponse::code).toList());
        assertTrue(codes.contains("1100000000"));
        assertTrue(codes.contains("2600000000"));
    }

    @Test
    void getTopLevelRegions_emptyList() {
        when(cacheService.getAllRegionsAsMap()).thenReturn(Collections.emptyMap());

        List<RegionResponse> result = regionService.getTopLevelRegions();

        assertTrue(result.isEmpty());
    }

    @Test
    void getChildRegions_returnsDtoList() {
        String parentCode = "1100000000";
        Region parent = new Region(parentCode, "서울특별시", null, 1);
        Region c1 = new Region("1100100000", "종로구", parentCode, 2);
        Region c2 = new Region("1100200000", "중구", parentCode, 2);
        Map<String, Region> map = new HashMap<>();
        map.put(parent.getCode(), parent);
        map.put(c1.getCode(), c1);
        map.put(c2.getCode(), c2);
        when(cacheService.getAllRegionsAsMap()).thenReturn(map);

        List<RegionResponse> result = regionService.getChildRegions(parentCode);

        assertEquals(2, result.size());
        Set<String> names = new HashSet<>(result.stream().map(RegionResponse::name).toList());
        assertTrue(names.contains("종로구"));
        assertTrue(names.contains("중구"));
    }

    @Test
    void getChildRegions_emptyList() {
        when(cacheService.getAllRegionsAsMap()).thenReturn(Collections.emptyMap());

        List<RegionResponse> result = regionService.getChildRegions("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("지역 계층 조회 성공")
    void getRegionHierarchy_success() {
        // Given
        Region seoul = new Region("1100000000", "서울특별시", null, 1);
        Region jongno = new Region("1100100000", "종로구", "1100000000", 2);
        Region cheongun = new Region("1100110100", "청운효자동", "1100100000", 3);

        Map<String, Region> map = Map.of(
            seoul.getCode(), seoul,
            jongno.getCode(), jongno,
            cheongun.getCode(), cheongun
        );
        when(cacheService.getAllRegionsAsMap()).thenReturn(map);

        // When
        List<RegionResponse> hierarchy = regionService.getRegionHierarchy("1100110100");

        // Then
        assertEquals(3, hierarchy.size());
        assertEquals("서울특별시", hierarchy.get(0).name());
        assertEquals("종로구", hierarchy.get(1).name());
        assertEquals("청운효자동", hierarchy.get(2).name());
    }

    @Test
    @DisplayName("존재하지 않는 지역 코드로 계층 조회 시 예외 발생")
    void getRegionHierarchy_notFound_throwsException() {
        when(cacheService.getAllRegionsAsMap()).thenReturn(Collections.emptyMap());

        assertThrows(RegionException.class, () -> regionService.getRegionHierarchy("invalid_code"));
    }

    @Test
    @DisplayName("지역 옵션 조회 - displayName 포함 확인")
    void getRegionOptions_includesDisplayName() {
        // Given
        Region seoul = new Region("1100000000", "서울특별시", null, 1);
        Region jongno = new Region("1100100000", "종로구", "1100000000", 2);
        Region cheongun = new Region("1100110100", "청운효자동", "1100100000", 3);

        Map<String, Region> map = Map.of(
            seoul.getCode(), seoul,
            jongno.getCode(), jongno,
            cheongun.getCode(), cheongun
        );
        when(cacheService.getAllRegionsAsMap()).thenReturn(map);

        // When
        List<RegionOptionResponse> options = regionService.getRegionOptions();

        // Then
        assertEquals(1, options.size()); // cheongun만 리프 노드
        RegionOptionResponse option = options.get(0);

        assertEquals("1100110100", option.code());
        assertEquals("서울특별시 > 종로구 > 청운효자동", option.displayName());
        assertEquals(List.of("1100000000", "1100100000", "1100110100"), option.pathCodes());
        assertEquals(List.of("서울특별시", "종로구", "청운효자동"), option.pathNames());
    }
}
