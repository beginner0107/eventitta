package com.eventitta.meeting.mapper;


import com.eventitta.meeting.domain.Meeting;
import com.eventitta.meeting.domain.MeetingStatus;
import com.eventitta.meeting.dto.MeetingCreateRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", imports = MeetingStatus.class)
public interface MeetingMapper {

    Meeting toEntity(MeetingCreateRequest request, Long userId);
}
