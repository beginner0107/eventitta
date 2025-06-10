package com.eventitta.event.service;

import com.eventitta.event.dto.request.NearbyEventRequest;
import com.eventitta.event.dto.response.EventDistanceDto;
import com.eventitta.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    public Page<EventDistanceDto> getNearbyEvents(
        NearbyEventRequest request
    ) {
        Pageable pageable = PageRequest.of(request.page(), request.size());
        return eventRepository.findEventsWithinDistanceAndDateBetween(
            request.lat(), request.lng(), request.distanceKm(),
            request.getStartDateTime(), request.getEndDateTime(),
            pageable
        );
    }
}
