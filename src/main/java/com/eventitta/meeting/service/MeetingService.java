package com.eventitta.meeting.service;

import com.eventitta.common.response.PageResponse;
import com.eventitta.gamification.event.ActivityEventPublisher;
import com.eventitta.meeting.constants.MeetingConstants;
import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingParticipant;
import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.domain.ParticipantStatus;
import com.eventitta.meeting.dto.request.MeetingCreateRequest;
import com.eventitta.meeting.dto.request.MeetingFilter;
import com.eventitta.meeting.dto.request.MeetingUpdateRequest;
import com.eventitta.meeting.dto.response.JoinMeetingResponse;
import com.eventitta.meeting.dto.response.MeetingDetailResponse;
import com.eventitta.meeting.dto.response.MeetingSummaryResponse;
import com.eventitta.meeting.dto.response.ParticipantResponse;
import com.eventitta.meeting.mapper.MeetingMapper;
import com.eventitta.meeting.repository.MeetingParticipantRepository;
import com.eventitta.meeting.repository.MeetingRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.eventitta.gamification.domain.ActivityType.JOIN_MEETING;
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
    private final ActivityEventPublisher activityEventPublisher;

    @Transactional
    public Long createMeeting(Long userId, MeetingCreateRequest request) {
        User leader = findUserById(userId);
        validateMeetingTime(request);

        Meeting meeting = meetingMapper.toEntity(request, leader);
        Meeting savedMeeting = meetingRepository.save(meeting);

        addLeaderAsParticipant(savedMeeting, leader);
        return savedMeeting.getId();
    }

    @Transactional
    public void updateMeeting(Long userId, Long meetingId, MeetingUpdateRequest request) {
        findUserById(userId);

        Meeting meeting = findMeetingById(meetingId);
        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }
        validateMeetingLeader(meeting, userId);
        validateMeetingTimeForUpdate(request);

        validateMaxMembersForUpdate(request, meeting);

        meeting.update(request);
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
            .map(participant -> meetingMapper.toParticipantResponse(participant, participant.getUser()))
            .collect(Collectors.toList());

        return meetingMapper.toDetailResponse(meeting, participantResponses);
    }

    public PageResponse<MeetingSummaryResponse> getMeetings(MeetingFilter filter) {
        Pageable pageReq = PageRequest.of(filter.page(), filter.size());
        Page<MeetingSummaryResponse> page = meetingRepository.findMeetingsByFilter(filter, pageReq);
        return PageResponse.of(page);
    }

    @Transactional
    public JoinMeetingResponse joinMeeting(Long userId, Long meetingId) {
        User user = findUserById(userId);
        Meeting meeting = findMeetingById(meetingId);

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }
        if (meeting.getStatus() != MeetingStatus.RECRUITING) {
            throw MEETING_NOT_RECRUITING.defaultException();
        }

        int approvedCount = participantRepository.findByMeetingAndStatus(meeting, ParticipantStatus.APPROVED).size();
        if (approvedCount >= meeting.getMaxMembers()) {
            throw MEETING_FULL.defaultException();
        }

        Optional<MeetingParticipant> existingParticipant = participantRepository.findByMeetingIdAndUser_Id(meetingId, userId);

        if (existingParticipant.isPresent()) {
            throw ALREADY_JOINED_MEETING.defaultException();
        }

        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .user(user)
            .status(ParticipantStatus.PENDING)
            .build();

        MeetingParticipant savedParticipant = participantRepository.save(participant);

        return new JoinMeetingResponse(
            savedParticipant.getId(),
            meetingId,
            savedParticipant.getStatus(),
            MeetingConstants.JOIN_MEETING_PENDING_MESSAGE
        );
    }

    @Transactional
    public ParticipantResponse approveParticipant(Long userId, Long meetingId, Long participantId) {
        findUserById(userId);

        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
            .orElseThrow(MEETING_NOT_FOUND::defaultException);

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }
        validateMeetingLeader(meeting, userId);

        MeetingParticipant participant = findParticipantById(participantId);
        validateParticipantBelongsToMeeting(participant, meetingId);
        validateParticipantStatus(participant);

        if (meeting.getCurrentMembers() >= meeting.getMaxMembers()) {
            throw MEETING_FULL.defaultException();
        }

        participant.approve();
        meeting.incrementCurrentMembers();

        activityEventPublisher.publish(JOIN_MEETING, participant.getUser().getId(), meetingId);

        return meetingMapper.toParticipantResponse(participant, participant.getUser());
    }

    @Transactional
    public ParticipantResponse rejectParticipant(Long userId, Long meetingId, Long participantId) {
        MeetingParticipant participant = validateParticipantManagement(userId, meetingId, participantId);

        participant.reject();

        return meetingMapper.toParticipantResponse(participant, participant.getUser());
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

        LocalDateTime now = LocalDateTime.now();
        if (!request.startTime().isAfter(now) || !request.endTime().isAfter(now)) {
            throw INVALID_MEETING_TIME.defaultException();
        }
    }

    private void validateMeetingTime(MeetingCreateRequest request) {
        if (request.endTime().isBefore(request.startTime())) {
            throw INVALID_MEETING_TIME.defaultException();
        }
    }

    private void addLeaderAsParticipant(Meeting meeting, User leader) {
        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .user(leader)
            .status(ParticipantStatus.APPROVED)
            .build();

        participantRepository.save(participant);
    }

    private void validateMaxMembersForUpdate(MeetingUpdateRequest request, Meeting meeting) {
        if (request.maxMembers() < meeting.getCurrentMembers()) {
            throw TOO_SMALL_MAX_MEMBERS.defaultException();
        }
    }

    private MeetingParticipant findParticipantById(Long participantId) {
        return participantRepository.findByIdWithMeeting(participantId)
            .orElseThrow(PARTICIPANT_NOT_FOUND::defaultException);
    }

    private void validateParticipantBelongsToMeeting(MeetingParticipant participant, Long meetingId) {
        if (!participant.getMeeting().getId().equals(meetingId)) {
            throw PARTICIPANT_NOT_IN_MEETING.defaultException();
        }
    }

    private void validateParticipantStatus(MeetingParticipant participant) {
        if (participant.getStatus() != ParticipantStatus.PENDING) {
            throw INVALID_PARTICIPANT_STATUS.defaultException();
        }
    }

    private MeetingParticipant validateParticipantManagement(Long userId, Long meetingId, Long participantId) {
        findUserById(userId);
        Meeting meeting = findMeetingById(meetingId);

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }
        validateMeetingLeader(meeting, userId);

        MeetingParticipant participant = findParticipantById(participantId);
        validateParticipantBelongsToMeeting(participant, meetingId);
        validateParticipantStatus(participant);
        return participant;
    }

    @Transactional
    public void cancelJoin(Long userId, Long meetingId) {
        findUserById(userId);

        Meeting meeting = findMeetingById(meetingId);

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }

        MeetingParticipant participant = participantRepository
            .findByMeetingIdAndUser_Id(meetingId, userId)
            .orElseThrow(PARTICIPANT_NOT_FOUND::defaultException);

        if (participant.getStatus() == ParticipantStatus.REJECTED) {
            throw INVALID_PARTICIPANT_STATUS.defaultException();
        }

        boolean wasApproved = participant.getStatus() == ParticipantStatus.APPROVED;

        if (wasApproved) {
            meeting.decrementCurrentMembers();

            activityEventPublisher.publishRevoke(JOIN_MEETING, userId, meetingId);
        }
        participantRepository.delete(participant);
    }

}
