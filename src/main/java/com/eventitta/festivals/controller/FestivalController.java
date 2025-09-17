package com.eventitta.festivals.controller;

import com.eventitta.common.response.PageResponse;
import com.eventitta.festivals.dto.FestivalResponseDto;
import com.eventitta.festivals.dto.NearbyFestivalRequest;
import com.eventitta.festivals.service.FestivalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이벤트", description = "지역 기반 이벤트 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/festivals")
public class FestivalController {

    private final FestivalService festivalService;

    @Operation(summary = "반경 내 이벤트 조회"
        , description = "위도, 경도, 거리, 기간, 페이징 파라미터로 반경 내 이벤트를 조회합니다.")
    @GetMapping("/nearby")
    public ResponseEntity<PageResponse<FestivalResponseDto>> getNearbyEvents(
        @ParameterObject @Valid @ModelAttribute NearbyFestivalRequest request
    ) {
        PageResponse<FestivalResponseDto> page = festivalService.getNearbyFestival(request);
        return ResponseEntity.ok(page);
    }
}
