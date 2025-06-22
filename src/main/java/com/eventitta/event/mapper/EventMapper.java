package com.eventitta.event.mapper;

import com.eventitta.event.dto.response.EventDistanceDto;
import com.eventitta.event.dto.response.EventResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "place", target = "place")
    @Mapping(source = "startTime", target = "startTime")
    @Mapping(source = "endTime", target = "endTime")
    @Mapping(source = "category", target = "category")
    @Mapping(source = "isFree", target = "isFree")
    @Mapping(source = "homepageUrl", target = "homepageUrl")
    @Mapping(source = "distance", target = "distance")
    EventResponseDto toResponse(EventDistanceDto src);
}
