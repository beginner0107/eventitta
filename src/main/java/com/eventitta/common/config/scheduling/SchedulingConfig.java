package com.eventitta.common.config.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정
 *
 * <p>각 스케줄러는 개별적으로 활성화/비활성화할 수 있습니다.
 * <ul>
 *   <li>토큰 정리: {@code scheduler.token-cleanup.enabled}</li>
 *   <li>축제 동기화: {@code scheduler.festival-sync.enabled}</li>
 *   <li>미팅 상태: {@code scheduler.meeting-status.enabled}</li>
 *   <li>이미지 정리: {@code scheduler.image-cleanup.enabled}</li>
 *   <li>실패 이벤트: {@code scheduler.failed-event-retry.enabled}</li>
 *   <li>랭킹 동기화: {@code scheduler.ranking-sync.enabled}</li>
 * </ul>
 *
 * <p>모든 스케줄러는 기본적으로 활성화되어 있으며 ({@code matchIfMissing = true}),
 * 특정 스케줄러만 비활성화하려면 해당 설정을 {@code false}로 지정하세요.
 *
 * @see com.eventitta.auth.scheduler.RefreshTokenCleanupTask
 * @see com.eventitta.festivals.scheduler.FestivalScheduler
 * @see com.eventitta.meeting.scheduler.MeetingStatusScheduler
 * @see com.eventitta.post.scheduler.PostImageFileScheduler
 * @see com.eventitta.gamification.scheduler.FailedActivityEventRetryScheduler
 * @see com.eventitta.gamification.scheduler.RankingScheduler
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
