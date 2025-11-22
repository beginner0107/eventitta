package com.eventitta.meeting.repository;

import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long>, MeetingRepositoryCustom {

    @Modifying
    @Query("UPDATE Meeting m SET m.status = :status WHERE m.endTime <= :now AND m.status <> :status AND m.deleted = false")
    int updateStatusToFinished(@Param("status") MeetingStatus status, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT m FROM Meeting m WHERE m.id = :id")
    Optional<Meeting> findByIdForUpdate(@Param("id") Long id);
}
