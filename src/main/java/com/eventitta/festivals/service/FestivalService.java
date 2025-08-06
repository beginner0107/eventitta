package com.eventitta.festivals.service;

import com.eventitta.common.response.PageResponse;
import com.eventitta.festivals.dto.FestivalResponseDto;
import com.eventitta.festivals.dto.NearbyFestivalsRequest;
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

    public PageResponse<FestivalResponseDto> getNearbyFestival(NearbyFestivalsRequest req) {
        var page = festivalRepository.findFestivalsWithinDistanceAndDateBetween(
            req.latitude(), req.longitude(), req.getDistanceKm(),
            req.getStartDateTime(), req.getEndDateTime(),
            PageRequest.of(req.page(), req.size())
        );

        return PageResponse.of(page);
    }
}
