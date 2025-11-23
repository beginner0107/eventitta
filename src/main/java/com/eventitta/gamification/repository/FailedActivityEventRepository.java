package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.FailedActivityEvent;
import com.eventitta.gamification.domain.FailedActivityEvent.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FailedActivityEventRepository extends JpaRepository<FailedActivityEvent, Long> {

    Page<FailedActivityEvent> findByStatus(EventStatus status, Pageable pageable);

    List<FailedActivityEvent> findByStatusAndRetryCountLessThan(EventStatus status, Integer maxRetryCount);

    @Query("SELECT f FROM FailedActivityEvent f WHERE f.status = :status AND f.failedAt > :since ORDER BY f.failedAt DESC")
    List<FailedActivityEvent> findRecentFailures(@Param("status") EventStatus status, @Param("since") LocalDateTime since);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE FailedActivityEvent f SET f.status = :newStatus WHERE f.status = :oldStatus AND f.id IN :ids")
    void updateStatusBatch(@Param("ids") List<Long> ids, @Param("oldStatus") EventStatus oldStatus, @Param("newStatus") EventStatus newStatus);
}

