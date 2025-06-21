package com.eventitta.meeting.mapper;


import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.dto.MeetingCreateRequest;
import com.eventitta.meeting.dto.MeetingUpdateRequest;
import com.eventitta.user.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

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

    @Mapping(target = "leader", ignore = true)
    @Mapping(target = "currentMembers", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "participants", ignore = true)
    void updateMeetingFromDto(MeetingUpdateRequest dto, @MappingTarget Meeting meeting);
}
