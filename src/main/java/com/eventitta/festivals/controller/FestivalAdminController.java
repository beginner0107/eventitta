package com.eventitta.festivals.controller;

import com.eventitta.festivals.service.FestivalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 축제 데이터 관리용 Admin API
 * 초기 데이터 적재 및 수동 동기화를 위한 엔드포인트 제공
 * <p>
 * 보안 설정: SecurityConfig에서 환경별 권한 제어
 */
@Tag(name = "관리자 - 축제 데이터", description = "축제 데이터 수동 동기화 API (관리자 전용)")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/festivals")
public class FestivalAdminController {

    private final FestivalService festivalService;

    /**
     * 전국 축제 데이터 수동 동기화
     * 전국문화행사표준데이터 API를 호출하여 데이터베이스에 저장
     * <p>
     * 사용 시나리오:
     * - 초기 데이터 적재
     * - 분기별 스케줄러 실패 시 수동 재실행
     * - 긴급 데이터 업데이트 필요 시
     * <p>
     * 보안: SecurityConfig에서 운영 환경 ADMIN 권한 검증 적용됨
     */
    @Operation(
        summary = "전국 축제 데이터 수동 동기화",
        description = "전국문화행사표준데이터 API를 호출하여 데이터베이스에 저장합니다. " +
            "초기 데이터 적재 또는 스케줄러 실패 시 사용합니다."
    )
    @PostMapping("/sync/national")
    public ResponseEntity<Map<String, String>> syncNationalFestivalData() {
        log.info("[Admin] 전국 축제 데이터 수동 동기화 요청");

        try {
            festivalService.loadInitialNationalFestivalData();
            log.info("[Admin] 전국 축제 데이터 수동 동기화 완료");

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "전국 축제 데이터 동기화가 완료되었습니다."
            ));
        } catch (Exception e) {
            log.error("[Admin] 전국 축제 데이터 수동 동기화 실패", e);

            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "전국 축제 데이터 동기화 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 서울시 축제 데이터 수동 동기화
     * 서울시 문화행사 API를 호출하여 데이터베이스에 저장
     * <p>
     * 사용 시나리오:
     * - 초기 데이터 적재
     * - 일별 스케줄러 실패 시 수동 재실행
     * - 긴급 데이터 업데이트 필요 시
     * <p>
     * 보안: SecurityConfig에서 운영 환경 ADMIN 권한 검증 적용됨
     */
    @Operation(
        summary = "서울시 축제 데이터 수동 동기화",
        description = "서울시 문화행사 API를 호출하여 데이터베이스에 저장합니다. " +
            "초기 데이터 적재 또는 스케줄러 실패 시 사용합니다."
    )
    @PostMapping("/sync/seoul")
    public ResponseEntity<Map<String, String>> syncSeoulFestivalData() {
        log.info("[Admin] 서울시 축제 데이터 수동 동기화 요청");

        try {
            festivalService.loadInitialSeoulFestivalData();
            log.info("[Admin] 서울시 축제 데이터 수동 동기화 완료");

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "서울시 축제 데이터 동기화가 완료되었습니다."
            ));
        } catch (Exception e) {
            log.error("[Admin] 서울시 축제 데이터 수동 동기화 실패", e);

            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "서울시 축제 데이터 동기화 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}
