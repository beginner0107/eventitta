package com.eventitta.region.service;

import com.eventitta.region.domain.Region;
import com.eventitta.region.dto.RegionDto;
import com.eventitta.region.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    private RegionRepository regionRepository;
    private RegionService regionService;

    @BeforeEach
    void setUp() {
        regionRepository = mock(RegionRepository.class);
        regionService = new RegionService(regionRepository);
    }

    @Test
    void getTopLevelRegions_returnsDtoList() {
        Region r1 = new Region("1100000000", "서울특별시", null, 1);
        Region r2 = new Region("2600000000", "부산광역시", null, 1);
        when(regionRepository.findByParentCodeIsNullOrderByNameAsc()).thenReturn(List.of(r1, r2));

        List<RegionDto> result = regionService.getTopLevelRegions();

        assertEquals(2, result.size());
        assertEquals("1100000000", result.get(0).code());
        assertEquals("서울특별시", result.get(0).name());
        assertEquals(1, result.get(0).level());
        assertEquals("2600000000", result.get(1).code());
        assertEquals("부산광역시", result.get(1).name());
    }

    @Test
    void getTopLevelRegions_emptyList() {
        when(regionRepository.findByParentCodeIsNullOrderByNameAsc()).thenReturn(List.of());

        List<RegionDto> result = regionService.getTopLevelRegions();

        assertTrue(result.isEmpty());
    }

    @Test
    void getChildRegions_returnsDtoList() {
        String parentCode = "1100000000";
        Region c1 = new Region("1100100000", "종로구", parentCode, 2);
        Region c2 = new Region("1100200000", "중구", parentCode, 2);
        when(regionRepository.findByParentCodeOrderByNameAsc(parentCode)).thenReturn(List.of(c1, c2));

        List<RegionDto> result = regionService.getChildRegions(parentCode);

        assertEquals(2, result.size());
        assertEquals("1100100000", result.get(0).code());
        assertEquals("종로구", result.get(0).name());
        assertEquals(2, result.get(0).level());
        assertEquals("1100200000", result.get(1).code());
        assertEquals("중구", result.get(1).name());
    }

    @Test
    void getChildRegions_emptyList() {
        String parentCode = "unknown";
        when(regionRepository.findByParentCodeOrderByNameAsc(parentCode)).thenReturn(List.of());

        List<RegionDto> result = regionService.getChildRegions(parentCode);

        assertTrue(result.isEmpty());
    }
}
