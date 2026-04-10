package com.eventitta.domain.meeting.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.meeting.domain.Meeting;
import com.eventitta.domain.meeting.domain.MeetingParticipant;
import com.eventitta.domain.meeting.domain.ParticipantStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface MeetingParticipantRepository extends BaseRepository<MeetingParticipant, Long> {

    @Query("""
        SELECT p.meeting.id
        FROM MeetingParticipant p
        WHERE p.userId = :userId
          AND p.meeting.deleted = false
        ORDER BY p.meeting.id ASC
        """)
    List<Long> findActiveMeetingIdsByUserId(@Param("userId") Long userId);

    /**
     * 특정 모임의 특정 상태를 가진 참여자 목록을 조회합니다.
     */
    List<MeetingParticipant> findByMeetingAndStatus(Meeting meeting, ParticipantStatus status);

    Optional<MeetingParticipant> findFirstByMeetingIdAndStatusAndUserIdNotOrderByCreatedAtAscIdAsc(
        Long meetingId,
        ParticipantStatus status,
        Long userId
    );

    /**
     * 특정 사용자의 특정 모임 참여 정보를 조회합니다.
     * JPA의 속성 경로 탐색을 사용합니다.
     */
    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

    /**
     * 참가자 ID로 참가자 정보를 조회하며, 연관된 모임 정보도 함께 조회합니다.
     */
    @Query("SELECT p FROM MeetingParticipant p JOIN FETCH p.meeting WHERE p.id = :participantId")
    Optional<MeetingParticipant> findByIdWithMeeting(@Param("participantId") Long participantId);

    /**
     * 특정 모임의 전체 참가자 수를 조회합니다.
     */
    int countByMeetingId(Long id);

    /**
     * 특정 모임의 특정 상태를 가진 참가자 수를 조회합니다.
     */
    int countByMeetingIdAndStatus(Long id, ParticipantStatus participantStatus);
}
