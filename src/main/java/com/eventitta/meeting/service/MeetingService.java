package com.eventitta.meeting.service;

import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingParticipant;
import com.eventitta.meeting.domain.ParticipantStatus;
import com.eventitta.meeting.dto.*;
import com.eventitta.meeting.mapper.MeetingMapper;
import com.eventitta.meeting.repository.MeetingParticipantRepository;
import com.eventitta.meeting.repository.MeetingRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
        User leader = findUserById(userId);
        validateMeetingTime(request);

        Meeting meeting = meetingMapper.toEntity(request, leader);
        Meeting savedMeeting = meetingRepository.save(meeting);

        addLeaderAsParticipant(savedMeeting, userId);
        return savedMeeting.getId();
    }

    @Transactional
    public void updateMeeting(Long userId, Long meetingId, MeetingUpdateRequest request) {
        findUserById(userId);

        Meeting meeting = findMeetingById(meetingId);
        validateMeetingLeader(meeting, userId);
        validateMeetingTimeForUpdate(request);

        validateMaxMembersForUpdate(request, meeting);

        meetingMapper.updateMeetingFromDto(request, meeting);
    }

    @Transactional
    public void deleteMeeting(Long userId, Long meetingId) {
        findUserById(userId);

        Meeting meeting = findMeetingById(meetingId);

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }

        validateMeetingLeader(meeting, userId);

        meeting.delete();
    }

    public MeetingDetailResponse getMeetingDetail(Long meetingId) {
        Meeting meeting = findMeetingById(meetingId);

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }

        List<MeetingParticipant> approvedParticipants = participantRepository
            .findByMeetingAndStatus(meeting, ParticipantStatus.APPROVED);

        List<ParticipantResponse> participantResponses = approvedParticipants.stream()
            .map(participant -> {
                User user = userRepository.findById(participant.getUserId()).orElse(null);
                return meetingMapper.toParticipantResponse(participant, user);
            })
            .collect(Collectors.toList());

        return meetingMapper.toDetailResponse(meeting, participantResponses);
    }

    private Meeting findMeetingById(Long meetingId) {
        return meetingRepository.findById(meetingId)
            .orElseThrow(MEETING_NOT_FOUND::defaultException);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
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
