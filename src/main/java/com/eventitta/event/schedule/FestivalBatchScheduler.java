package com.eventitta.event.schedule;

import com.eventitta.event.service.NationalFestivalImportService;
import com.eventitta.event.service.SeoulFestivalImportService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Year;

import static java.time.LocalDate.now;

@Slf4j
@Component
public class FestivalBatchScheduler {

    private final NationalFestivalImportService nationalService;
    private final SeoulFestivalImportService seoulService;

    @Value("${festival.national.schedule-cron}")
    private String nationalCron;   // "0 0 0 1 1,4,7,10 *"

    @Value("${festival.seoul.schedule-cron}")
    private String seoulCron;      // "0 0 11 * * *"

    public FestivalBatchScheduler(NationalFestivalImportService nationalService,
                                  SeoulFestivalImportService seoulService) {
        this.nationalService = nationalService;
        this.seoulService = seoulService;
    }

    /**
     * 전국 축제: 분기별(1,4,7,10월 1일 00:00) Upsert
     */
    @Scheduled(cron = "${festival.national.schedule-cron}", zone = "Asia/Seoul")
    @SchedulerLock(name = "runNationalQuarterlyImport")
    public void runNationalQuarterlyImport() {
        log.info("[Scheduler] 전국 축제 (분기별) upsert 시작");
        try {
            nationalService.importAll();
            log.info("[Scheduler] 전국 축제 upsert 완료");
        } catch (Exception e) {
            log.error("[Scheduler] 전국 축제 (분기별) upsert 중 예외 발생", e);
        }
    }

    /**
     * 서울시 축제: 매일 11시 현재 월(YYYY-MM) 단위 Upsert
     */
    @Scheduled(cron = "${festival.seoul.schedule-cron}", zone = "Asia/Seoul")
    @SchedulerLock(name = "runSeoulDailyImport")
    public void runSeoulDailyImport() {
        String ym = Year.now().getValue() + "-" + String.format("%02d", now().getMonthValue());
        log.info("[Scheduler] 서울시 축제 ({} Upsert) 시작", ym);
        try {
            seoulService.importCurrentMonth();
            log.info("[Scheduler] 서울시 축제 ({} Upsert) 완료", ym);
        } catch (Exception e) {
            log.error("[Scheduler] 서울시 축제 upsert 중 예외 발생", e);
        }
    }
}
