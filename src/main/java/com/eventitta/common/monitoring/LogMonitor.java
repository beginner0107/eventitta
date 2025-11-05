package com.eventitta.common.monitoring;

import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 로그 파일 모니터링 컴포넌트
 *
 * 기능:
 * - 주기적으로 에러 로그 파일을 체크하여 비정상적인 에러 발생 감지
 * - 로그 파일 크기 모니터링
 * - 임계치 초과 시 Slack 알림
 *
 * 프리티어 최적화:
 * - 파일 I/O를 최소화하여 디스크 부하 감소
 * - 메모리 효율적인 스트림 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "monitoring.log.enabled", havingValue = "true", matchIfMissing = false)
public class LogMonitor {

    private final SlackNotificationService slackNotificationService;

    @Value("${logging.file.path:/app/logs}")
    private String logPath;

    @Value("${monitoring.log.error-threshold:10}")
    private int errorThreshold;

    @Value("${monitoring.log.time-window-minutes:5}")
    private int timeWindowMinutes;

    @Value("${monitoring.log.size-threshold-mb:800}")
    private long logSizeThresholdMb;

    @Value("${monitoring.log.alert-cooldown-minutes:30}")
    private int alertCooldownMinutes;

    // 마지막 알림 시간 (중복 알림 방지)
    private Instant lastErrorAlert = Instant.MIN;
    private Instant lastSizeAlert = Instant.MIN;

    // 로그 타임스탬프 파싱용 (Asia/Seoul 타임존)
    private static final DateTimeFormatter LOG_TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 1분마다 에러 로그 모니터링
     * 프리티어 환경에서는 I/O를 최소화하기 위해 간격 조정 가능
     */
    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    public void monitorErrorLogs() {
        try {
            Path errorLogPath = Paths.get(logPath, "error.log");

            if (!Files.exists(errorLogPath)) {
                log.debug("에러 로그 파일이 존재하지 않음: {}", errorLogPath);
                return;
            }

            int recentErrorCount = countRecentErrors(errorLogPath);

            if (recentErrorCount >= errorThreshold) {
                sendErrorThresholdAlert(recentErrorCount);
            }

        } catch (Exception e) {
            log.error("에러 로그 모니터링 중 오류 발생", e);
        }
    }

    /**
     * 10분마다 로그 디렉토리 크기 체크
     */
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void monitorLogDirectorySize() {
        try {
            Path logDirectory = Paths.get(logPath);

            if (!Files.exists(logDirectory)) {
                log.debug("로그 디렉토리가 존재하지 않음: {}", logDirectory);
                return;
            }

            long totalSizeMB = calculateDirectorySize(logDirectory);

            if (totalSizeMB >= logSizeThresholdMb) {
                sendLogSizeAlert(totalSizeMB);
            }

        } catch (Exception e) {
            log.error("로그 디렉토리 크기 체크 중 오류 발생", e);
        }
    }

    /**
     * 최근 N분간 에러 로그 개수 카운트
     */
    private int countRecentErrors(Path errorLogPath) throws IOException {
        Instant cutoffTime = Instant.now().minus(timeWindowMinutes, ChronoUnit.MINUTES);
        AtomicInteger errorCount = new AtomicInteger(0);

        try (Stream<String> lines = Files.lines(errorLogPath)) {
            lines
                .filter(line -> line.contains("ERROR"))
                .filter(line -> isWithinTimeWindow(line, cutoffTime))
                .forEach(line -> errorCount.incrementAndGet());
        }

        return errorCount.get();
    }

    /**
     * 로그 라인이 특정 시간 이후인지 확인
     * 로그는 Asia/Seoul 타임존으로 기록되므로 올바른 타임존 변환 필요
     */
    private boolean isWithinTimeWindow(String logLine, Instant cutoffTime) {
        try {
            // 로그 패턴: 2025-10-21 15:30:00.123 [thread] ERROR ...
            if (logLine.length() < 23) {
                return false;
            }

            String timestampStr = logLine.substring(0, 23); // "2025-10-21 15:30:00.123"

            // Asia/Seoul 타임존으로 파싱
            LocalDateTime localDateTime = LocalDateTime.parse(timestampStr, LOG_TIMESTAMP_FORMATTER);
            Instant logTime = localDateTime.atZone(SEOUL_ZONE).toInstant();

            return logTime.isAfter(cutoffTime);
        } catch (Exception e) {
            // 타임스탬프 파싱 실패 시 최근 로그로 간주
            log.trace("로그 타임스탬프 파싱 실패: {}", logLine, e);
            return true;
        }
    }

    /**
     * 디렉토리 전체 크기 계산 (MB 단위)
     */
    private long calculateDirectorySize(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files
                .filter(Files::isRegularFile)
                .mapToLong(this::getFileSize)
                .sum() / (1024 * 1024); // Bytes to MB
        }
    }

    /**
     * 파일 크기 반환 (에러 시 0)
     */
    private long getFileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 에러 임계치 초과 알림 전송
     */
    private void sendErrorThresholdAlert(int errorCount) {
        // 중복 알림 방지: 마지막 알림 후 N분 이내에는 재전송 안 함
        if (Instant.now().isBefore(lastErrorAlert.plus(alertCooldownMinutes, ChronoUnit.MINUTES))) {
            log.debug("에러 알림 쿨다운 기간 중 - 알림 생략");
            return;
        }

        String message = String.format(
            "최근 %d분간 ERROR 로그가 %d건 발생했습니다 (임계치: %d건). 서버 상태를 확인해주세요.",
            timeWindowMinutes, errorCount, errorThreshold
        );

        slackNotificationService.sendAlert(
            AlertLevel.HIGH,
            "LOG_ERROR_THRESHOLD",
            message,
            "/monitoring/logs",
            "LogMonitor",
            null
        );

        lastErrorAlert = Instant.now();
        log.warn("에러 로그 임계치 초과 알림 전송: {}건", errorCount);
    }

    /**
     * 로그 디렉토리 크기 초과 알림 전송
     */
    private void sendLogSizeAlert(long totalSizeMB) {
        // 중복 알림 방지
        if (Instant.now().isBefore(lastSizeAlert.plus(alertCooldownMinutes, ChronoUnit.MINUTES))) {
            log.debug("로그 크기 알림 쿨다운 기간 중 - 알림 생략");
            return;
        }

        String message = String.format(
            "로그 디렉토리 크기가 %d MB입니다 (임계치: %d MB). 디스크 용량을 확인하고 오래된 로그를 정리해주세요.",
            totalSizeMB, logSizeThresholdMb
        );

        slackNotificationService.sendAlert(
            AlertLevel.MEDIUM,
            "LOG_SIZE_THRESHOLD",
            message,
            "/monitoring/logs",
            "LogMonitor",
            null
        );

        lastSizeAlert = Instant.now();
        log.warn("로그 디렉토리 크기 초과 알림 전송: {} MB", totalSizeMB);
    }

    /**
     * 현재 로그 상태 정보 반환 (모니터링용)
     */
    public String getLogStatus() {
        try {
            Path logDirectory = Paths.get(logPath);
            if (!Files.exists(logDirectory)) {
                return "로그 디렉토리 없음";
            }

            long totalSizeMB = calculateDirectorySize(logDirectory);
            Path errorLogPath = Paths.get(logPath, "error.log");
            int recentErrors = Files.exists(errorLogPath) ? countRecentErrors(errorLogPath) : 0;

            return String.format(
                "로그 디렉토리: %s, 전체 크기: %d MB, 최근 %d분간 에러: %d건",
                logPath, totalSizeMB, timeWindowMinutes, recentErrors
            );
        } catch (Exception e) {
            return "로그 상태 조회 실패: " + e.getMessage();
        }
    }
}
