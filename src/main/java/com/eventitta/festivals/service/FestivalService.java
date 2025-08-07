package com.eventitta.festivals.service;

import com.eventitta.common.response.PageResponse;
import com.eventitta.festivals.dto.FestivalResponseDto;
import com.eventitta.festivals.dto.NearbyFestivalRequest;
import com.eventitta.festivals.exception.FestivalErrorCode;
import com.eventitta.festivals.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FestivalService {

    private final SeoulFestivalInitializer seoulFestivalInitializer;
    private final NationalFestivalInitializer nationalFestivalInitializer;
    private final FestivalRepository festivalRepository;

    public void loadInitialNationalFestivalData() {
        nationalFestivalInitializer.loadInitialData();
    }

    public void loadInitialSeoulFestivalData() {
        seoulFestivalInitializer.loadInitialData();
    }

    /**
     * 서울시 축제 데이터 일별 동기화 (오늘 날짜만)
     */
    public void syncDailySeoulFestivalData() {
        LocalDate today = LocalDate.now();
        log.info("서울시 축제 데이터 일별 동기화 시작 - 대상 날짜: {}", today);
        seoulFestivalInitializer.loadDataForDate(today);
        log.info("서울시 축제 데이터 일별 동기화 완료 - 대상 날짜: {}", today);
    }

    public PageResponse<FestivalResponseDto> getNearbyFestival(NearbyFestivalRequest req) {
        // 유효성 검증
        validateNearbyFestivalRequest(req);

        var page = festivalRepository.findFestivalsWithinDistanceAndDateBetween(
            req.latitude(), req.longitude(), req.distanceKm(),
            req.getStartDateTime(), req.getEndDateTime(),
            PageRequest.of(req.page(), req.size())
        );

        // Projection을 DTO로 변환
        var dtoPage = page.map(projection -> FestivalResponseDto.builder()
            .id(projection.getId())
            .title(projection.getTitle())
            .place(projection.getPlace())
            .startTime(projection.getStartTime().atStartOfDay()) // LocalDate를 LocalDateTime으로 변환
            .endTime(projection.getEndTime().atTime(23, 59, 59)) // LocalDate를 LocalDateTime으로 변환
            .category(projection.getCategory())
            .isFree(projection.getIsFree())
            .homepageUrl(projection.getHomepageUrl())
            .distance(projection.getDistance())
            .build());

        return PageResponse.of(dtoPage);
    }

    private void validateNearbyFestivalRequest(NearbyFestivalRequest req) {
        if (req.distanceKm() <= 0 || req.distanceKm() > 100) {
            throw FestivalErrorCode.INVALID_LOCATION_RANGE.defaultException();
        }

        if (req.getStartDateTime().isAfter(req.getEndDateTime())) {
            throw FestivalErrorCode.INVALID_DATE_RANGE.defaultException();
        }
    }
}
