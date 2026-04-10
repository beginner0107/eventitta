package com.eventitta.domain.meeting.service;

import com.eventitta.domain.common.response.PageResponse;
import com.eventitta.domain.gamification.api.internal.facade.GamificationInternalFacade;
import com.eventitta.domain.meeting.constants.MeetingConstants;
import com.eventitta.domain.meeting.domain.Meeting;
import com.eventitta.domain.meeting.domain.MeetingParticipant;
import com.eventitta.domain.meeting.domain.MeetingStatus;
import com.eventitta.domain.meeting.domain.ParticipantStatus;
import com.eventitta.domain.meeting.dto.request.MeetingCreateRequest;
import com.eventitta.domain.meeting.dto.request.MeetingFilter;
import com.eventitta.domain.meeting.dto.request.MeetingUpdateRequest;
import com.eventitta.domain.meeting.dto.response.JoinMeetingResponse;
import com.eventitta.domain.meeting.dto.response.MeetingDetailResponse;
import com.eventitta.domain.meeting.dto.response.MeetingSummaryResponse;
import com.eventitta.domain.meeting.dto.response.ParticipantResponse;
import com.eventitta.domain.meeting.mapper.MeetingMapper;
import com.eventitta.domain.meeting.repository.MeetingParticipantRepository;
import com.eventitta.domain.meeting.repository.MeetingRepository;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.view.UserProfileView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.eventitta.domain.meeting.exception.MeetingErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final MeetingMapper meetingMapper;
    private final UserInternalFacade userInternalFacade;
    private final GamificationInternalFacade gamificationFacade;

    @Transactional
    public Long createMeeting(Long userId, MeetingCreateRequest request) {
        log.info("[미팅 생성 시작] userId={}, title={}", userId, request.title());

        userInternalFacade.ensureActiveUser(userId);
        validateMeetingTime(request);

        Meeting meeting = meetingMapper.toEntity(request, userId);
        Meeting savedMeeting = meetingRepository.save(meeting);

        addLeaderAsParticipant(savedMeeting, userId);

        log.info("[미팅 생성 완료] userId={}, meetingId={}", userId, savedMeeting.getId());
        return savedMeeting.getId();
    }

    @Transactional
    public void updateMeeting(Long userId, Long meetingId, MeetingUpdateRequest request) {
        log.info("[미팅 수정 시작] userId={}, meetingId={}", userId, meetingId);

        userInternalFacade.ensureActiveUser(userId);

        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
            .orElseThrow(MEETING_NOT_FOUND::defaultException);
        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }
        validateMeetingLeader(meeting, userId);
        validateMeetingTimeForUpdate(request);

        validateMaxMembersForUpdate(request, meeting);

        meeting.update(request);

        log.info("[미팅 수정 완료] userId={}, meetingId={}", userId, meetingId);
    }

    @Transactional
    public void deleteMeeting(Long userId, Long meetingId) {
        log.info("[미팅 삭제 시작] userId={}, meetingId={}", userId, meetingId);

        userInternalFacade.ensureActiveUser(userId);

        Meeting meeting = findMeetingById(meetingId);

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }

        validateMeetingLeader(meeting, userId);

        meeting.delete();

        log.info("[미팅 삭제 완료] userId={}, meetingId={}", userId, meetingId);
    }

    public MeetingDetailResponse getMeetingDetail(Long meetingId) {
        Meeting meeting = findMeetingById(meetingId);

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }

        List<MeetingParticipant> approvedParticipants = participantRepository
            .findByMeetingAndStatus(meeting, ParticipantStatus.APPROVED);
        LinkedHashSet<Long> profileIds = new LinkedHashSet<>();
        profileIds.add(meeting.getLeaderId());
        approvedParticipants.stream()
            .map(MeetingParticipant::getUserId)
            .forEach(profileIds::add);
        Map<Long, UserProfileView> profiles = userInternalFacade.findUserProfiles(profileIds);

        List<ParticipantResponse> participantResponses = approvedParticipants.stream()
            .map(participant -> meetingMapper.toParticipantResponse(participant, profiles.get(participant.getUserId())))
            .collect(Collectors.toList());

        return meetingMapper.toDetailResponse(meeting, profiles.get(meeting.getLeaderId()), participantResponses);
    }

    public PageResponse<MeetingSummaryResponse> getMeetings(MeetingFilter filter) {
        Pageable pageReq = PageRequest.of(filter.page(), filter.size());
        Page<MeetingSummaryResponse> page = meetingRepository.findMeetingsByFilter(filter, pageReq);
        return PageResponse.of(page);
    }

    @Transactional
    public JoinMeetingResponse joinMeeting(Long userId, Long meetingId) {
        log.info("[미팅 참가 요청] userId={}, meetingId={}", userId, meetingId);

        userInternalFacade.ensureActiveUser(userId);
        Meeting meeting = findMeetingById(meetingId);

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }
        if (meeting.getStatus() != MeetingStatus.RECRUITING) {
            throw MEETING_NOT_RECRUITING.defaultException();
        }

        int approvedCount = participantRepository.countByMeetingIdAndStatus(meetingId, ParticipantStatus.APPROVED);
        if (approvedCount >= meeting.getMaxMembers()) {
            log.warn("[미팅 정원 초과] userId={}, meetingId={}, currentMembers={}, maxMembers={}",
                userId, meetingId, approvedCount, meeting.getMaxMembers());
            throw MEETING_FULL.defaultException();
        }

        Optional<MeetingParticipant> existingParticipant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);

        if (existingParticipant.isPresent()) {
            log.warn("[중복 참가 시도] userId={}, meetingId={}", userId, meetingId);
            throw ALREADY_JOINED_MEETING.defaultException();
        }

        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .userId(userId)
            .status(ParticipantStatus.PENDING)
            .build();

        MeetingParticipant savedParticipant = participantRepository.save(participant);

        log.info("[미팅 참가 요청 완료] userId={}, meetingId={}, participantId={}, status={}",
            userId, meetingId, savedParticipant.getId(), savedParticipant.getStatus());

        return new JoinMeetingResponse(
            savedParticipant.getId(),
            meetingId,
            savedParticipant.getStatus(),
            MeetingConstants.JOIN_MEETING_PENDING_MESSAGE
        );
    }

    @Transactional
    public ParticipantResponse approveParticipant(Long userId, Long meetingId, Long participantId) {
        log.info("[미팅 참가 승인 시작] leaderId={}, meetingId={}, participantId={}",
            userId, meetingId, participantId);

        userInternalFacade.ensureActiveUser(userId);

        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
            .orElseThrow(MEETING_NOT_FOUND::defaultException);

        MeetingParticipant participant = validateAndGetPendingParticipant(
            meeting, userId, participantId
        );

        validateMeetingCapacity(meeting);

        participant.approve();
        meeting.incrementCurrentMembers();

        gamificationFacade.onMeetingJoinApproved(participant.getUserId(), meetingId);

        log.info("[미팅 참가 승인 완료] leaderId={}, meetingId={}, participantId={}, participantUserId={}",
            userId, meetingId, participantId, participant.getUserId());

        UserProfileView participantProfile = userInternalFacade.findUserProfile(participant.getUserId()).orElse(null);
        return meetingMapper.toParticipantResponse(participant, participantProfile);
    }

    @Transactional
    public ParticipantResponse rejectParticipant(Long userId, Long meetingId, Long participantId) {
        log.info("[미팅 참가 거부 시작] leaderId={}, meetingId={}, participantId={}",
            userId, meetingId, participantId);

        userInternalFacade.ensureActiveUser(userId);

        Meeting meeting = findMeetingById(meetingId);

        MeetingParticipant participant = validateAndGetPendingParticipant(
            meeting, userId, participantId
        );

        participant.reject();

        log.info("[미팅 참가 거부 완료] leaderId={}, meetingId={}, participantId={}, participantUserId={}",
            userId, meetingId, participantId, participant.getUserId());

        UserProfileView participantProfile = userInternalFacade.findUserProfile(participant.getUserId()).orElse(null);
        return meetingMapper.toParticipantResponse(participant, participantProfile);
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

    private void addLeaderAsParticipant(Meeting meeting, Long leaderId) {
        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .userId(leaderId)
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

    private MeetingParticipant validateAndGetPendingParticipant(
        Meeting meeting, Long userId, Long participantId) {

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }
        validateMeetingLeader(meeting, userId);

        MeetingParticipant participant = findParticipantById(participantId);
        validateParticipantBelongsToMeeting(participant, meeting.getId());
        validateParticipantStatus(participant);

        return participant;
    }

    private void validateMeetingCapacity(Meeting meeting) {
        if (meeting.getCurrentMembers() >= meeting.getMaxMembers()) {
            throw MEETING_FULL.defaultException();
        }
    }

    @Transactional
    public void cancelJoin(Long userId, Long meetingId) {
        log.info("[미팅 참가 취소 시작] userId={}, meetingId={}", userId, meetingId);

        userInternalFacade.ensureActiveUser(userId);

        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
            .orElseThrow(MEETING_NOT_FOUND::defaultException);

        if (meeting.isDeleted()) {
            throw ALREADY_DELETED_MEETING.defaultException();
        }

        MeetingParticipant participant = participantRepository
            .findByMeetingIdAndUserId(meetingId, userId)
            .orElseThrow(PARTICIPANT_NOT_FOUND::defaultException);

        if (participant.getStatus() == ParticipantStatus.REJECTED) {
            throw INVALID_PARTICIPANT_STATUS.defaultException();
        }

        boolean wasApproved = participant.getStatus() == ParticipantStatus.APPROVED;

        if (wasApproved) {
            meeting.decrementCurrentMembers();
            gamificationFacade.onMeetingJoinCancelled(userId, meetingId);
        }

        participantRepository.delete(participant);

        log.info("[미팅 참가 취소 완료] userId={}, meetingId={}, wasApproved={}",
            userId, meetingId, wasApproved);
    }

}
