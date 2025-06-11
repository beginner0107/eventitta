package com.eventitta.event.service;

import com.eventitta.common.response.PageResponse;
import com.eventitta.event.dto.request.NearbyEventRequest;
import com.eventitta.event.dto.response.EventResponseDto;
import com.eventitta.event.mapper.EventMapper;
import com.eventitta.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    public PageResponse<EventResponseDto> getNearbyEvents(NearbyEventRequest req) {
        var page = eventRepository.findEventsWithinDistanceAndDateBetween(
            req.lat(), req.lng(), req.distanceKm(),
            req.getStartDateTime(), req.getEndDateTime(),
            PageRequest.of(req.page(), req.size())
        );
        return PageResponse.map(page, eventMapper::toResponse);
    }
}
