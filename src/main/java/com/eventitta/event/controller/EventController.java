package com.eventitta.event.controller;

import com.eventitta.event.dto.EventDistanceDto;
import com.eventitta.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

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
        @RequestParam(value = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(value = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = Optional.ofNullable(from).orElse(today);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        LocalDateTime endDateTime = Optional.ofNullable(to)
            .map(d -> d.atTime(LocalTime.MAX))
            .orElse(LocalDateTime.of(9999, 12, 31, 23, 59, 59));

        Page<EventDistanceDto> result = eventService.getNearbyEvents(
            lat, lng, distanceKm,
            startDateTime, endDateTime,
            page, size
        );
        return ResponseEntity.ok(result);
    }
}
