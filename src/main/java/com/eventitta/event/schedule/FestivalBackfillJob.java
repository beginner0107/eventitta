package com.eventitta.event.schedule;

import com.eventitta.event.service.NationalFestivalImportService;
import com.eventitta.event.service.SeoulFestivalImportService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
//@Component
public class FestivalBackfillJob {

    private final NationalFestivalImportService nationalService;
    private final SeoulFestivalImportService seoulService;

    public FestivalBackfillJob(NationalFestivalImportService nationalService,
                               SeoulFestivalImportService seoulService) {
        this.nationalService = nationalService;
        this.seoulService = seoulService;
    }

    /**
     * 초기 축제 데이터를 백필(Backfill)하는 작업.
     * 애플리케이션이 완전히 기동된 뒤에 한 번만 실행되도록 스케줄러에 등록함.
     * (예: 매일 새벽 2시 30분마다, 최초 1회 또는 주기별로 백필이 필요하다면 이곳을 수정)
     */
//    @Scheduled(cron = "0 30 2 * * *", zone = "Asia/Seoul")
//    @PostConstruct
    public void runBackfillOnce() {
        log.info("[InitialImportJob] 초기 축제 데이터 백필 시작");

        try {
            // (1) 전국 축제 전체 백필
            nationalService.importAll();
            log.info("[InitialImportJob] 전국 축제 전체 upsert 완료");

            // (2) 서울시 축제: “현재 시점 기준으로 1년 전부터” 월별 백필
            YearMonth endYm = YearMonth.now();
            YearMonth startYm = endYm.minusYears(1);

            log.info("[InitialImportJob] 서울시 축제 백필 범위: {} → {}", startYm, endYm);
            for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
                String ymStr = ym.toString();
                log.debug("[InitialImportJob] 서울시 축제({}) upsert 시작", ymStr);
                try {
                    seoulService.importByYearMonth(ymStr);
                    log.debug("[InitialImportJob] 서울시 축제({}) upsert 완료", ymStr);
                } catch (Exception innerEx) {
                    // “특정 월”에 데이터가 없거나 에러가 나도, 전체 백필을 멈추면 안 되므로 로그만 찍고 계속 진행
                    log.warn("[InitialImportJob] 서울시 축제({}) upsert 중 예외 발생(해당 월 건너뜀)", ymStr, innerEx);
                }
            }
        } catch (Exception e) {
            log.error("[InitialImportJob] 초기 축제 백필 중 예외 발생", e);
        }

        log.info("[InitialImportJob] 초기 축제 데이터 백필 완료");
    }
}

