package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.ActivityOutbox;
import com.eventitta.gamification.domain.ActivityOutbox.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityOutboxRepository extends JpaRepository<ActivityOutbox, Long> {

    /**
     * PENDING 상태이고 재시도 횟수 미만인 아웃박스 레코드 조회 (처리 대상)
     * 생성 시각 순으로 정렬하여 오래된 이벤트부터 처리
     */
    @Query("SELECT o FROM ActivityOutbox o WHERE o.status = :status AND o.retryCount < :maxRetry ORDER BY o.createdAt ASC")
    List<ActivityOutbox> findPendingEvents(
            @Param("status") OutboxStatus status,
            @Param("maxRetry") int maxRetry,
            Pageable pageable);

    /**
     * Pessimistic Lock을 사용한 조회로 동시성 제어
     */
    @Query("SELECT o FROM ActivityOutbox o WHERE o.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ActivityOutbox> findByIdWithLock(@Param("id") Long id);

    /**
     * PROCESSING 상태에서 stuck된 레코드를 일괄 PENDING으로 되돌림
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ActivityOutbox o SET o.status = 'PENDING' WHERE o.status = 'PROCESSING' AND o.updatedAt < :threshold")
    int revertStuckProcessing(@Param("threshold") LocalDateTime threshold);

    /**
     * 처리 완료된 레코드 정리 (housekeeping)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ActivityOutbox o WHERE o.status = 'DONE' AND o.processedAt < :threshold")
    int deleteProcessedBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * 상태별 건수 조회 (모니터링용)
     */
    long countByStatus(OutboxStatus status);
}
