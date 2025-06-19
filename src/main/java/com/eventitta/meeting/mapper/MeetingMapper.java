package com.eventitta.meeting.mapper;


import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.dto.MeetingCreateRequest;
import com.eventitta.meeting.dto.MeetingUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", imports = MeetingStatus.class)
public interface MeetingMapper {

    Meeting toEntity(MeetingCreateRequest request, Long userId);

    void updateMeetingFromDto(MeetingUpdateRequest updateRequest, @MappingTarget Meeting meeting);
}
