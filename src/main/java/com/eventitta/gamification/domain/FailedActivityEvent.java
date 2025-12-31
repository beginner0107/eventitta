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
@Table(name = "failed_activity_events", indexes = {
    @Index(name = "idx_failed_event_status", columnList = "status"),
    @Index(name = "idx_failed_event_user", columnList = "user_id"),
    @Index(name = "idx_failed_event_created", columnList = "created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FailedActivityEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status = EventStatus.PENDING;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    public FailedActivityEvent(Long userId, ActivityType activityType, OperationType operationType,
                               Long targetId, String errorMessage) {
        this.userId = userId;
        this.activityType = activityType;
        this.operationType = operationType;
        this.targetId = targetId;
        this.failedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.retryCount = 0;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markAsProcessed() {
        this.status = EventStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markAsProcessing() {
        this.status = EventStatus.PROCESSING;
    }

    public void revertToPending() {
        this.status = EventStatus.PENDING;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public enum EventStatus {
        PENDING,    // 재처리 대기
        PROCESSING, // 처리 중
        PROCESSED,  // 처리 완료
        FAILED      // 최종 실패
    }
}
