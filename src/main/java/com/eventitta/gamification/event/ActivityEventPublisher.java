package com.eventitta.gamification.event;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.service.ActivityOutboxWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 활동 이벤트 발행 컴포넌트.
 * 기존 인메모리 이벤트 발행 대신 Transactional Outbox에 기록합니다.
 * 호출자의 트랜잭션에 참여하여 비즈니스 데이터와 원자적으로 저장됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventPublisher {

    private final ActivityOutboxWriter activityOutboxWriter;

    public void publish(ActivityType activityType, Long userId, Long targetId) {
        activityOutboxWriter.write(activityType, userId, targetId, OperationType.RECORD);
    }

    public void publishRevoke(ActivityType activityType, Long userId, Long targetId) {
        activityOutboxWriter.write(activityType, userId, targetId, OperationType.REVOKE);
    }
}
