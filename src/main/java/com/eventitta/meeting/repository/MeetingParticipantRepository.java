package com.eventitta.meeting.repository;

import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingParticipant;
import com.eventitta.meeting.domain.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    /**
     * 특정 모임의 특정 상태를 가진 참여자 목록을 조회합니다.
     */
    List<MeetingParticipant> findByMeetingAndStatus(Meeting meeting, ParticipantStatus status);

    /**
     * 특정 사용자의 특정 모임 참여 정보를 조회합니다.
     * JPA의 속성 경로 탐색을 사용합니다.
     */
    Optional<MeetingParticipant> findByMeetingIdAndUser_Id(Long meetingId, Long userId);

    /**
     * 참가자 ID로 참가자 정보를 조회하며, 연관된 모임 정보도 함께 조회합니다.
     */
    @Query("SELECT p FROM MeetingParticipant p JOIN FETCH p.meeting WHERE p.id = :participantId")
    Optional<MeetingParticipant> findByIdWithMeeting(@Param("participantId") Long participantId);
}
