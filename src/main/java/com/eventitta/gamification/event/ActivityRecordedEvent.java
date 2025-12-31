package com.eventitta.gamification.event;

import com.eventitta.gamification.domain.ActivityType;

/**
 * 활동이 성공적으로 기록된 후 발행되는 이벤트
 * 뱃지, 랭킹 등 부가 작업을 비동기로 처리하기 위함
 */
public record ActivityRecordedEvent(
    Long userId,
    Long activityId,
    ActivityType activityType,
    Integer points,
    Long targetId
) {
}
