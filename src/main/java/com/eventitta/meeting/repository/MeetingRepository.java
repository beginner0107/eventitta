package com.eventitta.meeting.repository;

import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MeetingRepository extends JpaRepository<Meeting, Long>, MeetingRepositoryCustom {

    @Modifying
    @Query("UPDATE Meeting m SET m.status = :status WHERE m.endTime <= :now AND m.status <> :status AND m.deleted = false")
    int updateStatusToFinished(@Param("status") MeetingStatus status, @Param("now") LocalDateTime now);
}
