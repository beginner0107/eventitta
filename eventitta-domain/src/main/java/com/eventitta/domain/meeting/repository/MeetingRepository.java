package com.eventitta.domain.meeting.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.meeting.domain.Meeting;
import com.eventitta.domain.meeting.domain.MeetingStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface MeetingRepository extends BaseRepository<Meeting, Long>, MeetingRepositoryCustom {

    @Query("SELECT m.id FROM Meeting m WHERE m.leaderId = :leaderId AND m.deleted = false ORDER BY m.id ASC")
    List<Long> findActiveIdsByLeaderId(@Param("leaderId") Long leaderId);

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
