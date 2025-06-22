package com.eventitta.meeting.repository;

import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long>, MeetingRepositoryCustom {

    @Query("SELECT m FROM Meeting m WHERE m.id = :id AND m.deleted = false")
    Optional<Meeting> findByIdAndDeletedFalse(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Meeting m SET m.status = :status WHERE m.endTime <= :now AND m.status <> :status AND m.deleted = false")
    int updateStatusToFinished(@Param("status") MeetingStatus status, @Param("now") LocalDateTime now);
}
