package com.eventitta.domain.meeting.mapper;

import com.eventitta.domain.meeting.domain.Meeting;
import com.eventitta.domain.meeting.domain.MeetingParticipant;
import com.eventitta.domain.meeting.domain.MeetingStatus;
import com.eventitta.domain.meeting.dto.request.MeetingCreateRequest;
import com.eventitta.domain.meeting.dto.response.MeetingDetailResponse;
import com.eventitta.domain.meeting.dto.response.ParticipantResponse;
import com.eventitta.domain.user.api.internal.view.UserProfileView;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MeetingMapper {

    public Meeting toEntity(MeetingCreateRequest request, Long leaderId) {
        return Meeting.builder()
            .title(request.title())
            .description(request.description())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .maxMembers(request.maxMembers())
            .currentMembers(1)
            .address(request.address())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .status(MeetingStatus.RECRUITING)
            .leaderId(leaderId)
            .build();
    }

    public MeetingDetailResponse toDetailResponse(
        Meeting meeting,
        UserProfileView leaderProfile,
        List<ParticipantResponse> participants
    ) {
        return new MeetingDetailResponse(
            meeting.getId(),
            meeting.getTitle(),
            meeting.getDescription(),
            meeting.getStartTime(),
            meeting.getEndTime(),
            meeting.getMaxMembers(),
            meeting.getCurrentMembers(),
            meeting.getAddress(),
            meeting.getLatitude(),
            meeting.getLongitude(),
            meeting.getStatus(),
            meeting.getLeaderId(),
            leaderProfile != null ? leaderProfile.nickname() : "알 수 없음",
            leaderProfile != null ? leaderProfile.profilePictureUrl() : null,
            participants
        );
    }

    public ParticipantResponse toParticipantResponse(MeetingParticipant participant, UserProfileView userProfile) {
        return new ParticipantResponse(
            participant.getId(),
            participant.getUserId(),
            userProfile != null ? userProfile.nickname() : "알 수 없음",
            userProfile != null ? userProfile.profilePictureUrl() : null,
            participant.getStatus()
        );
    }
}
