package com.eventitta.meeting.scheduler;

import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @Transactional
    public void markFinishedMeetings() {
        LocalDateTime now = LocalDateTime.now();
        int updated = meetingRepository.updateStatusToFinished(MeetingStatus.FINISHED, now);
        log.info("종료된 모임 자동 업데이트: {}건", updated);
    }
}
