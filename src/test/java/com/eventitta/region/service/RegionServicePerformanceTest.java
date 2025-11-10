package com.eventitta.region.service;

import com.eventitta.region.domain.Region;
import com.eventitta.region.dto.response.RegionOptionResponse;
import com.eventitta.region.dto.response.RegionResponse;
import com.eventitta.region.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 캐싱 도입 전 RegionService 성능 측정 테스트
 *
 * <p>목적: 캐싱이 필요한 이유를  입증</p>
 *
 * <p>측정 항목:
 * <ul>
 *   <li>반복 호출 시 DB 쿼리 횟수</li>
 * </ul>
 * </p>
 *
 * <p>기대 결과:
 * <ul>
 *   <li>매 요청마다 findAll() 호출 → 캐싱 필요성 입증</li>
 * </ul>
 * </p>
 */
@DisplayName("캐싱 도입 전 RegionService 성능 측정")
@ExtendWith(MockitoExtension.class)
class RegionServicePerformanceTest {

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private RegionService regionService;

    private List<Region> mockRegions;

    @BeforeEach
    void setUp() {
        mockRegions = createMockRegionData();
    }

    @Test
    @DisplayName("10회 호출 시 DB 쿼리가 20번 실행된다")
    void getRegionOptions_calledMultipleTimes_callsDbQueries() {
        // given
        List<Region> leafRegions = mockRegions.stream()
            .filter(r -> r.getLevel() == 3)
            .toList();

        given(regionRepository.findLeafRegionsOrderByCode()).willReturn(leafRegions);
        given(regionRepository.findAll()).willReturn(mockRegions);

        // when
        int callCount = 10;
        for (int i = 0; i < callCount; i++) {
            regionService.getRegionOptions();
        }

        // then
        verify(regionRepository, times(callCount)).findLeafRegionsOrderByCode();
        verify(regionRepository, times(callCount)).findAll();
    }
    // ===== Helper Methods =====

    /**
     * 실제와 유사한 Mock 데이터 생성
     * 서울특별시 25개 구 + 각 구당 평균 15개 동 = 약 400개 지역
     */
    private List<Region> createMockRegionData() {
        List<Region> regions = new ArrayList<>();

        // 시/도 (서울, 부산 등 17개)
        for (int sido = 0; sido < 17; sido++) {
            String sidoCode = String.format("%02d00000000", sido + 11);
            regions.add(new Region(sidoCode, "시도" + sido, null, 1));

            // 시/군/구 (각 시/도당 평균 20개)
            for (int sigungu = 0; sigungu < 20; sigungu++) {
                String sigunguCode = String.format("%02d%02d000000", sido + 11, sigungu + 1);
                regions.add(new Region(sigunguCode, "구군" + sigungu, sidoCode, 2));

                // 읍/면/동 (각 구/군당 평균 10개)
                for (int dong = 0; dong < 10; dong++) {
                    String dongCode = String.format("%02d%02d%02d0000", sido + 11, sigungu + 1, dong + 1);
                    regions.add(new Region(dongCode, "동" + dong, sigunguCode, 3));
                }
            }
        }

        System.out.println("\n[Mock 데이터 생성]");
        System.out.println("- 총 지역 수: " + regions.size() + "개");
        System.out.println("- 구조: 17개 시/도 × 20개 구/군 × 10개 동 = " + (17 * 20 * 10) + "개");
        System.out.println("- 예상 메모리: 약 " + (regions.size() / 70) + "MB");

        return regions;
    }
}
