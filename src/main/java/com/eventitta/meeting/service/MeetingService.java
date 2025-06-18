package com.eventitta.meeting.service;

import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingParticipant;
import com.eventitta.meeting.domain.ParticipantStatus;
import com.eventitta.meeting.dto.MeetingCreateRequest;
import com.eventitta.meeting.exception.MeetingErrorCode;
import com.eventitta.meeting.exception.MeetingException;
import com.eventitta.meeting.mapper.MeetingMapper;
import com.eventitta.meeting.repository.MeetingParticipantRepository;
import com.eventitta.meeting.repository.MeetingRepository;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND);
        }
    }

    private void validateMeetingTime(MeetingCreateRequest request) {
        if (request.endTime().isBefore(request.startTime())) {
            throw new MeetingException(MeetingErrorCode.INVALID_MEETING_TIME);
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
}
