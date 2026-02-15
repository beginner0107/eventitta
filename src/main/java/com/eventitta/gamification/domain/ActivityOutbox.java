package com.eventitta.gamification.domain;

import com.eventitta.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "activity_outbox", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at"),
        @Index(name = "idx_outbox_user", columnList = "user_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityOutbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private OperationType operationType;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    public ActivityOutbox(String idempotencyKey, Long userId, ActivityType activityType,
            OperationType operationType, Long targetId) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.activityType = activityType;
        this.operationType = operationType;
        this.targetId = targetId;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public void markAsProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    public void markAsDone() {
        this.status = OutboxStatus.DONE;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = truncate(errorMessage);
        this.processedAt = LocalDateTime.now();
    }

    public void incrementRetryAndRevertToPending(String errorMessage) {
        this.retryCount++;
        this.status = OutboxStatus.PENDING;
        this.errorMessage = truncate(errorMessage);
    }

    public void revertToPending() {
        this.status = OutboxStatus.PENDING;
    }

    private String truncate(String message) {
        if (message == null)
            return null;
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    public enum OutboxStatus {
        PENDING, // 처리 대기
        PROCESSING, // 처리 중
        DONE, // 처리 완료
        FAILED // 최종 실패
    }
}
