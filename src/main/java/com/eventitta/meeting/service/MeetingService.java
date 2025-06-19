package com.eventitta.meeting.service;

import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingParticipant;
import com.eventitta.meeting.domain.ParticipantStatus;
import com.eventitta.meeting.dto.MeetingCreateRequest;
import com.eventitta.meeting.dto.MeetingUpdateRequest;
import com.eventitta.meeting.mapper.MeetingMapper;
import com.eventitta.meeting.repository.MeetingParticipantRepository;
import com.eventitta.meeting.repository.MeetingRepository;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.eventitta.meeting.exception.MeetingErrorCode.*;
import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final MeetingMapper meetingMapper;
    private final UserRepository userRepository;

    @Transactional
    public Long createMeeting(Long userId, MeetingCreateRequest request) {
        validateUser(userId);
        validateMeetingTime(request);

        Meeting meeting = meetingMapper.toEntity(request, userId);
        Meeting savedMeeting = meetingRepository.save(meeting);

        addLeaderAsParticipant(savedMeeting, userId);
        return savedMeeting.getId();
    }

    @Transactional
    public void updateMeeting(Long userId, Long meetingId, MeetingUpdateRequest request) {
        validateUser(userId);

        Meeting meeting = findMeetingById(meetingId);
        validateMeetingLeader(meeting, userId);
        validateMeetingTimeForUpdate(request);

        validateMaxMembersForUpdate(request, meeting);

        meeting.update(
            request.title(),
            request.description(),
            request.startTime(),
            request.endTime(),
            request.maxMembers(),
            request.address(),
            request.latitude(),
            request.longitude(),
            request.status()
        );
    }

    private Meeting findMeetingById(Long meetingId) {
        return meetingRepository.findById(meetingId)
            .orElseThrow(MEETING_NOT_FOUND::defaultException);
    }

    private void validateMeetingLeader(Meeting meeting, Long userId) {
        if (!meeting.isLeader(userId)) {
            throw NOT_MEETING_LEADER.defaultException();
        }
    }

    private void validateMeetingTimeForUpdate(MeetingUpdateRequest request) {
        if (request.endTime().isBefore(request.startTime())) {
            throw INVALID_MEETING_TIME.defaultException();
        }
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw NOT_FOUND_USER_ID.defaultException();
        }
    }

    private void validateMeetingTime(MeetingCreateRequest request) {
        if (request.endTime().isBefore(request.startTime())) {
            throw INVALID_MEETING_TIME.defaultException();
        }
    }

    private void addLeaderAsParticipant(Meeting meeting, Long userId) {
        MeetingParticipant leader = MeetingParticipant.builder()
            .meeting(meeting)
            .userId(userId)
            .status(ParticipantStatus.APPROVED)
            .build();

        participantRepository.save(leader);
    }

    private void validateMaxMembersForUpdate(MeetingUpdateRequest request, Meeting meeting) {
        if (request.maxMembers() < meeting.getCurrentMembers()) {
            throw TOO_SMALL_MAX_MEMBERS.defaultException();
        }
    }
}
