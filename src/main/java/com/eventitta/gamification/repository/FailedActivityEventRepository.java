package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.FailedActivityEvent;
import com.eventitta.gamification.domain.FailedActivityEvent.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FailedActivityEventRepository extends JpaRepository<FailedActivityEvent, Long> {

    Page<FailedActivityEvent> findByStatus(EventStatus status, Pageable pageable);

    List<FailedActivityEvent> findByStatusAndRetryCountLessThan(EventStatus status, Integer maxRetryCount);

    @Query("SELECT f FROM FailedActivityEvent f WHERE f.status = :status AND f.failedAt > :since ORDER BY f.failedAt DESC")
    List<FailedActivityEvent> findRecentFailures(@Param("status") EventStatus status,
            @Param("since") LocalDateTime since);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE FailedActivityEvent f SET f.status = :newStatus WHERE f.status = :oldStatus AND f.id IN :ids")
    void updateStatusBatch(@Param("ids") List<Long> ids, @Param("oldStatus") EventStatus oldStatus,
            @Param("newStatus") EventStatus newStatus);

    /**
     * Pessimistic Lock을 사용한 조회로 동시성 제어
     * 다른 트랜잭션이 이 레코드를 수정하지 못하도록 방지
     */
    @Query("SELECT e FROM FailedActivityEvent e WHERE e.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FailedActivityEvent> findByIdWithLock(@Param("id") Long id);

    /**
     * PROCESSING 상태에서 일정 시간 이상 stuck된 이벤트 조회
     * JVM 크래시 등으로 PROCESSING 상태에서 멈춘 레코드를 감지
     */
    @Query("SELECT f FROM FailedActivityEvent f WHERE f.status = 'PROCESSING' AND f.updatedAt < :threshold")
    List<FailedActivityEvent> findStuckProcessingEvents(@Param("threshold") LocalDateTime threshold);

    /**
     * PROCESSING stuck 이벤트를 일괄 PENDING으로 되돌림
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE FailedActivityEvent f SET f.status = 'PENDING' WHERE f.status = 'PROCESSING' AND f.updatedAt < :threshold")
    int revertStuckProcessing(@Param("threshold") LocalDateTime threshold);

    /**
     * 상태별 이벤트 수 조회 (모니터링용)
     */
    long countByStatus(EventStatus status);
}
