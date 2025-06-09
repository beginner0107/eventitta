package com.eventitta.event.controller;

import com.eventitta.event.dto.EventDistanceDto;
import com.eventitta.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    @GetMapping("/nearby")
    public ResponseEntity<Page<EventDistanceDto>> getNearbyEvents(
        @RequestParam("lat") double lat,
        @RequestParam("lng") double lng,
        @RequestParam("distanceKm") double distanceKm,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Page<EventDistanceDto> result = eventService.getNearbyEvents(lat, lng, distanceKm, page, size);
        return ResponseEntity.ok(result);
    }
}
