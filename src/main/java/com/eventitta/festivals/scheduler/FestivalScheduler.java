package com.eventitta.festivals.scheduler;

import com.eventitta.festivals.service.FestivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalScheduler {

    private final FestivalService festivalService;

    /**
     * 초기 수동 적재용: 전국문화행사표준데이터
     */
//    @PostConstruct
    public void loadInitialNationalFestivalData() {
        festivalService.loadInitialNationalFestivalData();
    }

    /**
     * 초기 수동 적재용: 서울
     */
//    @PostConstruct
    public void loadInitialSeoulFestivalData() {
        festivalService.loadInitialSeoulFestivalData();
    }

    /**
     * 전국 축제 데이터 정기 동기화
     * 분기별 실행: 1월, 4월, 7월, 10월 1일 오전 2시
     * 전국문화표준데이터는 분기별로 갱신되므로 분기마다 실행
     */
    @Scheduled(cron = "0 0 2 1 1,4,7,10 *", zone = "Asia/Seoul")
    @SchedulerLock(name = "syncNationalFestivalData", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public void syncNationalFestivalData() {
        log.info("[Scheduler] 전국 축제 데이터 분기별 동기화 시작");
        try {
            festivalService.loadInitialNationalFestivalData();
            log.info("[Scheduler] 전국 축제 데이터 분기별 동기화 완료");
        } catch (Exception e) {
            log.error("[Scheduler] 전국 축제 데이터 분기별 동기화 중 오류 발생", e);
            // 스케줄러에서는 예외를 다시 던지지 않고 로깅만 수행
        }
    }

    /**
     * 서울시 축제 데이터 정기 동기화
     * 매일 오전 3시 실행
     * 서울시 문화행사 정보는 매일 1회 업데이트되므로 오늘 날짜만 동기화
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "syncSeoulFestivalData", lockAtMostFor = "PT1H", lockAtLeastFor = "PT5M")
    public void syncSeoulFestivalData() {
        log.info("[Scheduler] 서울시 축제 데이터 일별 동기화 시작");
        try {
            festivalService.syncDailySeoulFestivalData();
            log.info("[Scheduler] 서울시 축제 데이터 일별 동기화 완료");
        } catch (Exception e) {
            log.error("[Scheduler] 서울시 축제 데이터 일별 동기화 중 오류 발생", e);
            // 스케줄러에서는 예외를 다시 던지지 않고 로깅만 수행
        }
    }
}
