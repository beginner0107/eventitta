package com.eventitta.meeting.scheduler;

import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingStatusScheduler {

    private final MeetingRepository meetingRepository;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @SchedulerLock(
        name = "markFinishedMeetings",
        lockAtMostFor = "PT5M",
        lockAtLeastFor = "PT30S"
    )
    @Transactional
    public void markFinishedMeetings() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int updated = meetingRepository.updateStatusToFinished(MeetingStatus.FINISHED, now);
            if (updated > 0) {
                log.info("[Scheduler] 종료된 미팅 상태 업데이트 완료 - 처리 건수: {}", updated);
            } else {
                log.debug("[Scheduler] 종료된 미팅 상태 변경 없음");
            }
        } catch (Exception e) {
            log.error("[Scheduler] 종료된 미팅 상태 업데이트 실패", e);
        }
    }
}
