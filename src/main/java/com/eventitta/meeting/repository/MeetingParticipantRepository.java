package com.eventitta.meeting.repository;

import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingParticipant;
import com.eventitta.meeting.domain.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    /**
     * 특정 모임의 특정 상태를 가진 참여자 목록을 조회합니다.
     */
    List<MeetingParticipant> findByMeetingAndStatus(Meeting meeting, ParticipantStatus status);

    /**
     * 특정 사용자의 특정 모임 참여 정보를 조회합니다.
     */
    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);
}
