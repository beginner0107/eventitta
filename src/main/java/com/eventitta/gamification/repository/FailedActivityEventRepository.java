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
    List<FailedActivityEvent> findRecentFailures(@Param("status") EventStatus status, @Param("since") LocalDateTime since);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE FailedActivityEvent f SET f.status = :newStatus WHERE f.status = :oldStatus AND f.id IN :ids")
    void updateStatusBatch(@Param("ids") List<Long> ids, @Param("oldStatus") EventStatus oldStatus, @Param("newStatus") EventStatus newStatus);

    /**
     * Pessimistic Lock을 사용한 조회로 동시성 제어
     * 다른 트랜잭션이 이 레코드를 수정하지 못하도록 방지
     */
    @Query("SELECT e FROM FailedActivityEvent e WHERE e.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FailedActivityEvent> findByIdWithLock(@Param("id") Long id);
}

