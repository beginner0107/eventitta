package com.eventitta.event.schedule;

import com.eventitta.event.service.NationalFestivalImportService;
import com.eventitta.event.service.SeoulFestivalImportService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
public class FestivalBatchScheduler {

    private final NationalFestivalImportService nationalService;
    private final SeoulFestivalImportService seoulService;

    public FestivalBatchScheduler(
        NationalFestivalImportService nationalService,
        SeoulFestivalImportService seoulService
    ) {
        this.nationalService = nationalService;
        this.seoulService = seoulService;
    }

    // 애플리케이션 기동 직후: 초기 로직
    //    - 전국 축제: 전체 Upsert (과거~미래)
    //    - 서울시 축제: 과거 2024년~현재(또는 원하는 시점)까지 연-월 단위로 Upsert
    @PostConstruct
    public void initAllDataOnStartup() {
        // (1) 전국 축제 전체 백필
        log.info("[Startup 백필] 전국 공공 축제 전체 Upsert 시작");
        try {
            nationalService.upsertAllNational();
            log.info("[Startup 백필] 전국 공공 축제 전체 Upsert 완료");
        } catch (Exception e) {
            log.error("[Startup 백필] 전국 공공 축제 Upsert 중 예외 발생", e);
        }

        // (2) 서울시 축제 과거 (예: 2024년 1월부터 현재 달까지)
        YearMonth startYm = YearMonth.of(2024, 1);
        YearMonth endYm = YearMonth.now(); // ex: 2025-06
        for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
            String ymStr = ym.toString();  // "2024-01", "2024-02", …, "2025-06"

            log.info("[Startup 백필] 서울시 축제({}) Upsert 시작", ymStr);
            try {
                seoulService.upsertByYearMonth(ymStr);
                log.info("[Startup 백필] 서울시 축제({}) Upsert 완료", ymStr);
            } catch (Exception e) {
                log.error("[Startup 백필] 서울시 축제({}) Upsert 중 예외 발생", ymStr, e);
            }
        }
    }

    // 전국 축제: 매일 10시 Upsert
    @Scheduled(cron = "0 0 0 1 1,4,7,10 *", zone = "Asia/Seoul")
    public void runNationalDailyUpsert() {
        log.info("[Scheduler] 전국 공공 축제 Upsert 시작");
        try {
            nationalService.upsertAllNational();
            log.info("[Scheduler] 전국 공공 축제 Upsert 완료");
        } catch (Exception e) {
            log.error("[Scheduler] 전국 공공 축제 Upsert 중 예외 발생", e);
        }
    }

    // 매일 해당 월(YYYY-MM)만 Upsert
    @Scheduled(cron = "0 0 11 * * *", zone = "Asia/Seoul")
    public void runSeoulDailyUpsertByMonth() {
        String ym = YearMonth.now().toString();
        log.info("[Scheduler] 서울시 문화행사 (월={} Upsert) 시작", ym);
        try {
            seoulService.upsertByYearMonth(ym);
            log.info("[Scheduler] 서울시 문화행사 (월={} Upsert) 완료", ym);
        } catch (Exception e) {
            log.error("[Scheduler] 서울시 문화행사 Upsert 중 예외 발생", e);
        }
    }

}
