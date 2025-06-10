package com.eventitta.event.service;

import com.eventitta.event.dto.EventDistanceDto;
import com.eventitta.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    public Page<EventDistanceDto> getNearbyEvents(
        double lat,
        double lng,
        double distanceKm,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findEventsWithinDistanceAndDateBetween(
            lat, lng, distanceKm,
            startDateTime, endDateTime,
            pageable
        );
    }
}
