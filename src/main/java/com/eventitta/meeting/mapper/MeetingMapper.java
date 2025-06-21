package com.eventitta.meeting.mapper;


import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingParticipant;
import com.eventitta.meeting.dto.MeetingCreateRequest;
import com.eventitta.meeting.dto.MeetingDetailResponse;
import com.eventitta.meeting.dto.ParticipantResponse;
import com.eventitta.user.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MeetingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currentMembers", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "participants", ignore = true)

    @Mapping(source = "request.title", target = "title")
    @Mapping(source = "request.description", target = "description")
    @Mapping(source = "request.startTime", target = "startTime")
    @Mapping(source = "request.endTime", target = "endTime")
    @Mapping(source = "request.maxMembers", target = "maxMembers")
    @Mapping(source = "request.address", target = "address")
    @Mapping(source = "request.latitude", target = "latitude")
    @Mapping(source = "request.longitude", target = "longitude")

    @Mapping(target = "status", constant = "RECRUITING")
    @Mapping(source = "leader", target = "leader")
    Meeting toEntity(MeetingCreateRequest request, User leader);

    @Mapping(source = "meeting.id", target = "id")
    @Mapping(source = "meeting.title", target = "title")
    @Mapping(source = "meeting.description", target = "description")
    @Mapping(source = "meeting.startTime", target = "startTime")
    @Mapping(source = "meeting.endTime", target = "endTime")
    @Mapping(source = "meeting.maxMembers", target = "maxMembers")
    @Mapping(source = "meeting.currentMembers", target = "currentMembers")
    @Mapping(source = "meeting.address", target = "address")
    @Mapping(source = "meeting.latitude", target = "latitude")
    @Mapping(source = "meeting.longitude", target = "longitude")
    @Mapping(source = "meeting.status", target = "status")
    @Mapping(source = "meeting.leader.id", target = "leaderId")
    @Mapping(source = "meeting.leader.nickname", target = "leaderNickname")
    @Mapping(source = "meeting.leader.profilePictureUrl", target = "leaderProfileUrl")
    @Mapping(source = "participants", target = "participants")
    MeetingDetailResponse toDetailResponse(Meeting meeting, List<ParticipantResponse> participants);

    @Mapping(source = "participant.id", target = "id")
    @Mapping(source = "participant.userId", target = "userId")
    @Mapping(source = "user.nickname", target = "nickname", defaultValue = "닉네임 없음")
    @Mapping(source = "user.profilePictureUrl", target = "profileUrl")
    @Mapping(source = "participant.status", target = "status")
    ParticipantResponse toParticipantResponse(MeetingParticipant participant, User user);
}
