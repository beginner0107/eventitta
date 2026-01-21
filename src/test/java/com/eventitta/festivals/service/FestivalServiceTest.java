package com.eventitta.festivals.service;

import com.eventitta.common.response.PageResponse;
import com.eventitta.festivals.dto.response.FestivalNearbyResponse;
import com.eventitta.festivals.dto.projection.FestivalProjection;
import com.eventitta.festivals.dto.request.NearbyFestivalRequest;
import com.eventitta.festivals.repository.FestivalRepository;
import com.eventitta.festivals.util.BoundingBox;
import com.eventitta.festivals.util.BoundingBoxCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("축제 비즈니스 로직 테스트")
class FestivalServiceTest {

    @Mock
    private SeoulFestivalInitializer seoulFestivalInitializer;

    @Mock
    private NationalFestivalInitializer nationalFestivalInitializer;

    @Mock
    private FestivalRepository festivalRepository;

    @Mock
    private BoundingBoxCalculator boundingBoxCalculator;

    @InjectMocks
    private FestivalService festivalService;

    @Test
    @DisplayName("전국 축제 데이터 가져오기 - 처음 시작할 때 전국 축제 정보를 불러온다")
    void givenNothing_whenLoadInitialNationalFestivalData_thenInitializerIsExecuted() {
        // given
        // 특별한 setup 없음

        // when
        festivalService.loadInitialNationalFestivalData();

        // then
        then(nationalFestivalInitializer).should(times(1)).loadInitialData();
    }

    @Test
    @DisplayName("서울시 축제 데이터 가져오기 - 처음 시작할 때 서울시 축제 정보를 불러온다")
    void givenNothing_whenLoadInitialSeoulFestivalData_thenInitializerIsExecuted() {
        // given
        // 특별한 setup 없음

        // when
        festivalService.loadInitialSeoulFestivalData();

        // then
        then(seoulFestivalInitializer).should(times(1)).loadInitialData();
    }

    @Test
    @DisplayName("서울시 축제 데이터 업데이트 - 매일 새로운 서울시 축제 정보로 업데이트한다")
    void givenToday_whenSyncDailySeoulFestivalData_thenTodayDataIsSynced() {
        // given
        LocalDate today = LocalDate.now();
        given(seoulFestivalInitializer.loadDataForDate(any(LocalDate.class)))
            .willReturn(new com.eventitta.festivals.service.processor.FestivalProcessor.ProcessingMetrics());

        // when
        festivalService.syncDailySeoulFestivalData();

        // then
        then(seoulFestivalInitializer).should(times(1)).loadDataForDate(eq(today));
    }

    @Test
    @DisplayName("내 주변 축제 찾기 - 위치와 조건을 입력하면 조건에 맞는 축제 목록을 보여준다")
    void givenNearbyFestivalsRequest_whenGetNearbyFestival_thenReturnsFestivalPage() {
        // given
        Double latitude = 37.5665;
        Double longitude = 126.9780;
        Double distanceKm = 5.0;
        LocalDate from = LocalDate.of(2025, 8, 1);
        LocalDate to = LocalDate.of(2025, 8, 31);
        int page = 0;
        int size = 20;

        NearbyFestivalRequest request = new NearbyFestivalRequest(
            latitude, longitude, distanceKm, from, to, page, size
        );

        // BoundingBox Mock 설정
        BoundingBox mockBox = new BoundingBox(37.5215, 37.6115, 126.9155, 127.0405);
        given(boundingBoxCalculator.calculate(latitude, longitude, distanceKm))
            .willReturn(mockBox);

        // FestivalProjection 모킹
        FestivalProjection festivalProjection = new FestivalProjection() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public String getTitle() {
                return "테스트 축제";
            }

            @Override
            public String getPlace() {
                return "테스트 장소";
            }

            @Override
            public LocalDate getStartTime() {
                return LocalDate.of(2025, 8, 15);
            }

            @Override
            public LocalDate getEndTime() {
                return LocalDate.of(2025, 8, 17);
            }

            @Override
            public String getCategory() {
                return "문화";
            }

            @Override
            public Boolean getIsFree() {
                return false;
            }

            @Override
            public String getHomepageUrl() {
                return "https://test.com";
            }

            @Override
            public Double getDistance() {
                return 3.2;
            }
        };

        PageImpl<FestivalProjection> mockPage = new PageImpl<>(
            List.of(festivalProjection),
            PageRequest.of(page, size),
            1L
        );

        given(festivalRepository.findFestivalsWithinDistanceAndDateBetween(
            eq(latitude),
            eq(longitude),
            eq(distanceKm),
            eq(mockBox.minLatitude()),
            eq(mockBox.maxLatitude()),
            eq(mockBox.minLongitude()),
            eq(mockBox.maxLongitude()),
            eq(request.getStartDateTime()),
            eq(request.getEndDateTime()),
            any(PageRequest.class)
        )).willReturn(mockPage);

        // when
        PageResponse<FestivalNearbyResponse> result = festivalService.getNearbyFestival(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getTitle()).isEqualTo("테스트 축제");
        assertThat(result.content().get(0).getDistance()).isEqualTo(3.2);
        assertThat(result.totalElements()).isEqualTo(1);

        // BoundingBoxCalculator 호출 검증
        then(boundingBoxCalculator).should(times(1))
            .calculate(latitude, longitude, distanceKm);

        // Repository 호출 검증
        then(festivalRepository).should(times(1))
            .findFestivalsWithinDistanceAndDateBetween(
                eq(latitude),
                eq(longitude),
                eq(distanceKm),
                eq(mockBox.minLatitude()),
                eq(mockBox.maxLatitude()),
                eq(mockBox.minLongitude()),
                eq(mockBox.maxLongitude()),
                eq(request.getStartDateTime()),
                eq(request.getEndDateTime()),
                eq(PageRequest.of(page, size))
            );
    }

    @Test
    @DisplayName("내 주변 축제 찾기 - 날짜 조건 없이 검색하면 오늘을 기준으로 해당하는 축제를 보여준다")
    void givenRequestWithoutDateRange_whenGetNearbyFestival_thenUsesDefaultDateRange() {
        // given
        Double latitude = 37.5665;
        Double longitude = 126.9780;
        Double distanceKm = 5.0;

        NearbyFestivalRequest request = new NearbyFestivalRequest(
            latitude, longitude, distanceKm, null, null, 0, 20
        );

        // BoundingBox Mock 설정
        BoundingBox mockBox = new BoundingBox(37.5215, 37.6115, 126.9155, 127.0405);
        given(boundingBoxCalculator.calculate(latitude, longitude, distanceKm))
            .willReturn(mockBox);

        PageImpl<FestivalProjection> mockPage = new PageImpl<>(List.of());

        given(festivalRepository.findFestivalsWithinDistanceAndDateBetween(
            any(Double.class),
            any(Double.class),
            any(Double.class),
            any(Double.class),
            any(Double.class),
            any(Double.class),
            any(Double.class),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(PageRequest.class)
        )).willReturn(mockPage);

        // when
        PageResponse<FestivalNearbyResponse> result = festivalService.getNearbyFestival(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content()).isEmpty();

        // 시작 시간이 오늘 시작으로 설정되는지 확인
        LocalDateTime expectedStart = LocalDate.now().atStartOfDay();
        LocalDateTime expectedEnd = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

        // BoundingBoxCalculator 호출 검증
        then(boundingBoxCalculator).should(times(1))
            .calculate(latitude, longitude, distanceKm);

        then(festivalRepository).should(times(1))
            .findFestivalsWithinDistanceAndDateBetween(
                eq(latitude),
                eq(longitude),
                eq(distanceKm),
                eq(mockBox.minLatitude()),
                eq(mockBox.maxLatitude()),
                eq(mockBox.minLongitude()),
                eq(mockBox.maxLongitude()),
                eq(expectedStart),
                eq(expectedEnd),
                eq(PageRequest.of(0, 20))
            );
    }

    @Test
    @DisplayName("내 주변 축제 찾기 - 페이지 설정 없이 검색하면 기본 설정(첫 페이지, 20개씩)으로 보여준다")
    void givenRequestWithNullPageParams_whenGetNearbyFestival_thenUsesDefaultPageParams() {
        // given
        Double latitude = 37.5665;
        Double longitude = 126.9780;
        Double distanceKm = 5.0;

        NearbyFestivalRequest request = new NearbyFestivalRequest(
            latitude, longitude, distanceKm, null, null, null, null
        );

        // BoundingBox Mock 설정
        BoundingBox mockBox = new BoundingBox(37.5215, 37.6115, 126.9155, 127.0405);
        given(boundingBoxCalculator.calculate(latitude, longitude, distanceKm))
            .willReturn(mockBox);

        PageImpl<FestivalProjection> mockPage = new PageImpl<>(List.of());

        given(festivalRepository.findFestivalsWithinDistanceAndDateBetween(
            any(Double.class),
            any(Double.class),
            any(Double.class),
            any(Double.class),
            any(Double.class),
            any(Double.class),
            any(Double.class),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(PageRequest.class)
        )).willReturn(mockPage);

        // when
        PageResponse<FestivalNearbyResponse> result = festivalService.getNearbyFestival(request);

        // then
        assertThat(result).isNotNull();

        // BoundingBoxCalculator 호출 검증
        then(boundingBoxCalculator).should(times(1))
            .calculate(latitude, longitude, distanceKm);

        // 기본 페이지 파라미터 (page=0, size=20)로 호출되는지 확인
        then(festivalRepository).should(times(1))
            .findFestivalsWithinDistanceAndDateBetween(
                any(Double.class),
                any(Double.class),
                any(Double.class),
                any(Double.class),
                any(Double.class),
                any(Double.class),
                any(Double.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(PageRequest.of(0, 20))
            );
    }
}
