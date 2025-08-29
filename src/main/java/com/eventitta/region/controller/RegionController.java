package com.eventitta.region.controller;

import com.eventitta.region.dto.RegionDto;
import com.eventitta.region.dto.RegionOptionDto;
import com.eventitta.region.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
@Tag(name = "지역 API", description = "지역 선택을 위한 API")
public class RegionController {
    private final RegionService regionService;

    @Operation(summary = "최상위 지역 목록 조회")
    @GetMapping
    public ResponseEntity<List<RegionDto>> getTopRegions() {
        List<RegionDto> regions = regionService.getTopLevelRegions();
        return ResponseEntity.ok(regions);
    }

    @Operation(summary = "하위 지역 목록 조회")
    @GetMapping("/{parentCode}")
    public ResponseEntity<List<RegionDto>> getChildRegions(
        @PathVariable String parentCode
    ) {
        List<RegionDto> regions = regionService.getChildRegions(parentCode);
        return ResponseEntity.ok(regions);
    }

    @Operation(summary = "지역 계층 조회", description = "특정 지역 코드로부터 최상위까지의 계층 구조를 조회합니다")
    @GetMapping("/{code}/hierarchy")
    public ResponseEntity<List<RegionDto>> getRegionHierarchy(
        @PathVariable String code
    ) {
        List<RegionDto> hierarchy = regionService.getRegionHierarchy(code);
        return ResponseEntity.ok(hierarchy);
    }

    @Operation(summary = "전체 지역 옵션 목록 조회")
    @GetMapping("/options")
    public ResponseEntity<List<RegionOptionDto>> getRegionOptions() {
        List<RegionOptionDto> options = regionService.getRegionOptions();
        return ResponseEntity.ok(options);
    }
}
