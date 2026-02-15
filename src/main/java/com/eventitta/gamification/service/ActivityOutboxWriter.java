package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityOutbox;
import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.repository.ActivityOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 비즈니스 트랜잭션 내에서 activity_outbox 테이블에 이벤트를 기록합니다.
 * 호출자의 트랜잭션에 참여(@Transactional)하므로,
 * 비즈니스 데이터 저장과 아웃박스 INSERT가 원자적으로 처리됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityOutboxWriter {

    private final ActivityOutboxRepository activityOutboxRepository;

    @Transactional
    public void write(ActivityType activityType, Long userId, Long targetId, OperationType operationType) {
        String idempotencyKey = generateIdempotencyKey(operationType, userId, activityType, targetId);

        ActivityOutbox outbox = ActivityOutbox.builder()
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .activityType(activityType)
                .operationType(operationType)
                .targetId(targetId)
                .build();

        activityOutboxRepository.save(outbox);

        log.debug("[Outbox 기록] idempotencyKey={}, userId={}, activityType={}, operationType={}",
                idempotencyKey, userId, activityType, operationType);
    }

    /**
     * 멱등키 생성
     * UUID를 포함하여 동일 사용자/타입/대상의 활동도 각각 구분
     */
    private String generateIdempotencyKey(OperationType op, Long userId, ActivityType type, Long targetId) {
        return String.format("%s:%d:%s:%d:%s",
                op, userId, type, targetId, UUID.randomUUID().toString().substring(0, 8));
    }
}
