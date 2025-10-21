package com.eventitta.common.monitoring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 로그 시스템 테스트용 컨트롤러
 *
 * 로컬/개발 환경에서만 활성화되며, 프로덕션에서는 비활성화됩니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/logs")
@Tag(name = "Log Test", description = "로그 시스템 테스트 API (개발 환경 전용)")
@Profile({"local", "test"})
public class LogTestController {

    @Autowired(required = false)
    private LogMonitor logMonitor;

    @GetMapping("/all-levels")
    @Operation(summary = "모든 로그 레벨 테스트", description = "TRACE부터 ERROR까지 모든 로그 레벨을 출력합니다")
    public ResponseEntity<Map<String, String>> testAllLogLevels() {
        log.trace("TRACE 레벨 로그 - 가장 상세한 디버그 정보");
        log.debug("DEBUG 레벨 로그 - 디버깅에 유용한 정보");
        log.info("INFO 레벨 로그 - 일반 정보성 메시지");
        log.warn("WARN 레벨 로그 - 경고 메시지");
        log.error("ERROR 레벨 로그 - 에러 메시지");

        Map<String, String> result = new HashMap<>();
        result.put("message", "모든 로그 레벨 출력 완료");
        result.put("logged", "TRACE, DEBUG, INFO, WARN, ERROR");
        result.put("check", "콘솔 또는 로그 파일을 확인하세요");

        return ResponseEntity.ok(result);
    }

    @GetMapping("/sanitizer")
    @Operation(summary = "LogSanitizer 테스트", description = "민감 정보 마스킹 기능을 테스트합니다")
    public ResponseEntity<Map<String, String>> testLogSanitizer() {
        String email = "user@example.com";
        String phone = "010-1234-5678";
        String token = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        String apiKey = "apikey=abc123def456ghi789jkl012mno345pqr678";
        String password = "password=mysecretpassword123";

        log.info("원본 이메일: {}", email);
        log.info("마스킹된 이메일: {}", LogSanitizer.sanitize(email));

        log.info("원본 전화번호: {}", phone);
        log.info("마스킹된 전화번호: {}", LogSanitizer.sanitize(phone));

        log.info("원본 JWT 토큰: {}", token);
        log.info("마스킹된 JWT 토큰: {}", LogSanitizer.sanitize(token));

        log.info("원본 API 키: {}", apiKey);
        log.info("마스킹된 API 키: {}", LogSanitizer.sanitize(apiKey));

        log.info("원본 비밀번호: {}", password);
        log.info("마스킹된 비밀번호: {}", LogSanitizer.sanitize(password));

        Map<String, String> result = new HashMap<>();
        result.put("message", "LogSanitizer 테스트 완료");
        result.put("check", "콘솔에서 원본과 마스킹된 값을 비교하세요");

        return ResponseEntity.ok(result);
    }

    @GetMapping("/error-simulation")
    @Operation(summary = "에러 로그 시뮬레이션", description = "연속적인 에러 로그를 생성하여 LogMonitor 알림을 테스트합니다")
    public ResponseEntity<Map<String, String>> simulateErrors(
        @RequestParam(defaultValue = "5") int count
    ) {
        for (int i = 1; i <= count; i++) {
            log.error("시뮬레이션 에러 #{} - LogMonitor 테스트용", i);
            try {
                Thread.sleep(100); // 100ms 간격
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Map<String, String> result = new HashMap<>();
        result.put("message", String.format("%d개의 에러 로그 생성 완료", count));
        result.put("warning", "LogMonitor는 5분간 10건 이상의 에러 발생 시 알림을 보냅니다");
        result.put("note", "10회 이상 호출하면 Slack 알림이 발송될 수 있습니다");

        return ResponseEntity.ok(result);
    }

    @GetMapping("/monitor-status")
    @Operation(summary = "로그 모니터 상태 조회", description = "현재 로그 시스템의 상태를 확인합니다")
    public ResponseEntity<Map<String, String>> getMonitorStatus() {
        String status = "LogMonitor 비활성화 (local 프로파일에서는 기본 비활성화)";

        // LogMonitor가 활성화된 경우에만 상태 조회
        try {
            if (logMonitor != null) {
                status = logMonitor.getLogStatus();
            }
        } catch (Exception e) {
            status = "LogMonitor 상태 조회 실패: " + e.getMessage();
        }

        Map<String, String> result = new HashMap<>();
        result.put("status", status);
        result.put("note", "LogMonitor를 테스트하려면 monitoring.log.enabled=true로 설정하세요");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/custom-message")
    @Operation(summary = "커스텀 로그 메시지", description = "원하는 레벨과 메시지로 로그를 출력합니다")
    public ResponseEntity<Map<String, String>> logCustomMessage(
        @RequestParam String level,
        @RequestParam String message,
        @RequestParam(defaultValue = "false") boolean sanitize
    ) {
        String logMessage = sanitize ? LogSanitizer.sanitize(message) : message;

        switch (level.toUpperCase()) {
            case "TRACE":
                log.trace(logMessage);
                break;
            case "DEBUG":
                log.debug(logMessage);
                break;
            case "INFO":
                log.info(logMessage);
                break;
            case "WARN":
                log.warn(logMessage);
                break;
            case "ERROR":
                log.error(logMessage);
                break;
            default:
                log.info(logMessage);
        }

        Map<String, String> result = new HashMap<>();
        result.put("level", level);
        result.put("message", logMessage);
        result.put("sanitized", String.valueOf(sanitize));

        return ResponseEntity.ok(result);
    }
}
