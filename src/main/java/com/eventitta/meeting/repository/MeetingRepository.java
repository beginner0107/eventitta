package com.eventitta.meeting.repository;

import com.eventitta.meeting.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long>, MeetingRepositoryCustom {

    @Query("SELECT m FROM Meeting m WHERE m.id = :id AND m.deleted = false")
    Optional<Meeting> findByIdAndDeletedFalse(@Param("id") Long id);
}
