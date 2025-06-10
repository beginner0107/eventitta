package com.eventitta.event.controller;

import com.eventitta.event.dto.request.NearbyEventRequest;
import com.eventitta.event.dto.response.EventDistanceDto;
import com.eventitta.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이벤트", description = "지역 기반 이벤트 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "반경 내 이벤트 조회"
        , description = "위도, 경도, 거리, 기간, 페이징 파라미터로 반경 내 이벤트를 조회합니다.")
    @GetMapping("/nearby")
    public ResponseEntity<Page<EventDistanceDto>> getNearbyEvents(
        @ParameterObject @Valid @ModelAttribute NearbyEventRequest request
    ) {
        Page<EventDistanceDto> page = eventService.getNearbyEvents(request);
        return ResponseEntity.ok(page);
    }
}
