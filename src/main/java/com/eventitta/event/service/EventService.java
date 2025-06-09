package com.eventitta.event.service;

import com.eventitta.event.dto.EventDistanceDto;
import com.eventitta.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    /**
     * 주어진 위도(lat), 경도(lng)에서 distanceKm 반경 내 이벤트를 페이징 조회
     */
    public Page<EventDistanceDto> getNearbyEvents(
        double lat,
        double lng,
        double distanceKm,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findEventsWithinDistance(lat, lng, distanceKm, pageable);
    }
}
