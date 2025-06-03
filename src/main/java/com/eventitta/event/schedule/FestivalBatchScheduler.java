package com.eventitta.event.schedule;

import com.eventitta.event.service.FestivalImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class FestivalBatchScheduler {

    private final FestivalImportService importService;

    public FestivalBatchScheduler(FestivalImportService importService) {
        this.importService = importService;
    }

    /**
     * 분기별 데이터 가져오기: RestClient + Upsert 로직 호출
     * cron: 매년 1,4,7,10월의 1일 00:00:00 에 실행
     */
//    @Scheduled(cron = "0 0 0 1 1,4,7,10 *", zone = "Asia/Seoul")
    @Scheduled(initialDelay = 60_000, fixedDelay = Long.MAX_VALUE, zone = "Asia/Seoul") // 테스트
    public void runQuarterlyUpsert() {
        try {
            importService.importAllFestivalsInsert();
            log.debug("[Scheduler] 축제 데이터 Upsert 완료: {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("[Scheduler] 업서트 중 예외 발생: {}", e.getMessage());
        }
    }
}
