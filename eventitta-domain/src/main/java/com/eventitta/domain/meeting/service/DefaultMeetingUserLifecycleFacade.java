package com.eventitta.domain.meeting.service;

import com.eventitta.domain.gamification.api.internal.facade.GamificationInternalFacade;
import com.eventitta.domain.meeting.api.internal.facade.MeetingUserLifecycleFacade;
import com.eventitta.domain.meeting.domain.Meeting;
import com.eventitta.domain.meeting.domain.MeetingParticipant;
import com.eventitta.domain.meeting.domain.ParticipantStatus;
import com.eventitta.domain.meeting.repository.MeetingParticipantRepository;
import com.eventitta.domain.meeting.repository.MeetingRepository;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class DefaultMeetingUserLifecycleFacade implements MeetingUserLifecycleFacade {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final GamificationInternalFacade gamificationFacade;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeUserFromActiveMeetings(Long userId) {
        List<Long> meetingIds = Stream.concat(
                meetingRepository.findActiveIdsByLeaderId(userId).stream(),
                participantRepository.findActiveMeetingIdsByUserId(userId).stream()
            )
            .distinct()
            .sorted()
            .toList();

        for (Long meetingId : meetingIds) {
            processMeeting(userId, meetingId);
        }
    }

    private void processMeeting(Long userId, Long meetingId) {
        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
        if (meeting == null || meeting.isDeleted()) {
            return;
        }

        MeetingParticipant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
            .orElse(null);

        if (meeting.isLeader(userId)) {
            processLeaderMeeting(meeting, participant, userId);
            return;
        }

        if (participant != null) {
            removeParticipant(
                meeting,
                participant,
                userId,
                participant.getStatus() == ParticipantStatus.APPROVED
            );
        }
    }

    private void processLeaderMeeting(Meeting meeting, MeetingParticipant leaderParticipant, Long userId) {
        int approvedCount = participantRepository.countByMeetingIdAndStatus(meeting.getId(), ParticipantStatus.APPROVED);
        if (approvedCount < 2) {
            meeting.delete();
            return;
        }

        MeetingParticipant successor = participantRepository
            .findFirstByMeetingIdAndStatusAndUserIdNotOrderByCreatedAtAscIdAsc(
                meeting.getId(),
                ParticipantStatus.APPROVED,
                userId
            )
            .orElse(null);

        if (successor == null) {
            meeting.delete();
            return;
        }

        meeting.changeLeader(successor.getUserId());

        if (leaderParticipant != null) {
            removeParticipant(meeting, leaderParticipant, userId, false);
        }
    }

    private void removeParticipant(
        Meeting meeting,
        MeetingParticipant participant,
        Long userId,
        boolean revokeJoinActivity
    ) {
        if (participant.getStatus() == ParticipantStatus.APPROVED) {
            meeting.decrementCurrentMembers();
            if (revokeJoinActivity) {
                gamificationFacade.onMeetingJoinCancelled(userId, meeting.getId());
            }
        }
        participantRepository.delete(participant);
    }
}
