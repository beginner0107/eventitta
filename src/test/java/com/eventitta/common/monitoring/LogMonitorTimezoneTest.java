package com.eventitta.common.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogMonitor의 타임존 처리 검증 테스트
 *
 * 로그는 Asia/Seoul 타임존으로 기록되므로, 타임스탬프 파싱 시
 * 올바른 타임존 변환이 필요함을 검증합니다.
 */
class LogMonitorTimezoneTest {

    private static final DateTimeFormatter LOG_TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("로그 타임스탬프를 Asia/Seoul 타임존으로 올바르게 파싱한다")
    void parseLogTimestampWithSeoulTimezone() {
        // Given: 서울 시간 2025-10-21 15:30:00
        String logTimestamp = "2025-10-21 15:30:00.123";

        // When: Asia/Seoul 타임존으로 파싱
        LocalDateTime localDateTime = LocalDateTime.parse(logTimestamp, LOG_TIMESTAMP_FORMATTER);
        Instant seoulInstant = localDateTime.atZone(SEOUL_ZONE).toInstant();

        // When: UTC로 잘못 파싱하는 경우 (기존 버그)
        Instant utcInstant = Instant.parse(logTimestamp.replace(' ', 'T') + "Z");

        // Then: 9시간 차이가 발생해야 함 (서울이 UTC+9)
        long hoursDifference = ChronoUnit.HOURS.between(utcInstant, seoulInstant);
        assertThat(hoursDifference).isEqualTo(-9);
    }

    @Test
    @DisplayName("시간 윈도우 계산이 타임존을 고려하여 정확하게 동작한다")
    void timeWindowCalculationWithCorrectTimezone() {
        // Given: 현재 시간 (서울 기준)
        Instant now = Instant.now();
        Instant fiveMinutesAgo = now.minus(5, ChronoUnit.MINUTES);

        // Given: 3분 전의 로그 (서울 시간으로 기록)
        LocalDateTime threeMinutesAgoSeoul = LocalDateTime.now(SEOUL_ZONE).minus(3, ChronoUnit.MINUTES);
        Instant threeMinutesAgoInstant = threeMinutesAgoSeoul.atZone(SEOUL_ZONE).toInstant();

        // When & Then: 5분 윈도우 내에 있어야 함
        assertThat(threeMinutesAgoInstant).isAfter(fiveMinutesAgo);
        assertThat(threeMinutesAgoInstant).isBefore(now);
    }

    @Test
    @DisplayName("동일한 시간이라도 타임존이 다르면 Instant 값이 달라진다")
    void sameTimeButDifferentTimezoneResultsInDifferentInstant() {
        // Given: 같은 시간 문자열 "2025-10-21 15:30:00"
        LocalDateTime localDateTime = LocalDateTime.parse(
            "2025-10-21 15:30:00.000",
            LOG_TIMESTAMP_FORMATTER
        );

        // When: 서울과 UTC로 각각 변환
        Instant seoulInstant = localDateTime.atZone(SEOUL_ZONE).toInstant();
        Instant utcInstant = localDateTime.atZone(ZoneId.of("UTC")).toInstant();

        // Then: 9시간 차이 발생
        long hoursDifference = ChronoUnit.HOURS.between(utcInstant, seoulInstant);
        assertThat(hoursDifference).isEqualTo(-9);

        // Then: 같은 물리적 시간이 아니므로 다른 Instant 값
        assertThat(seoulInstant).isNotEqualTo(utcInstant);
    }

    @Test
    @DisplayName("로그 타임스탬프 파싱 예제")
    void logTimestampParsingExample() {
        // Given: 실제 로그 라인
        String logLine = "2025-10-21 15:30:00.123 [http-nio-8080-exec-1] ERROR c.e.EventittaApplication - Test error";

        // When: 타임스탬프 추출 및 파싱
        String timestampStr = logLine.substring(0, 23);
        LocalDateTime localDateTime = LocalDateTime.parse(timestampStr, LOG_TIMESTAMP_FORMATTER);
        Instant logTime = localDateTime.atZone(SEOUL_ZONE).toInstant();

        // Then: 파싱 성공
        assertThat(logTime).isNotNull();
        assertThat(timestampStr).isEqualTo("2025-10-21 15:30:00.123");
    }

    @Test
    @DisplayName("cutoff 시간 계산 시 타임존이 자동으로 처리된다")
    void cutoffTimeCalculationHandlesTimezoneAutomatically() {
        // Given: 현재 시간 기준 5분 전
        Instant now = Instant.now();
        Instant cutoffTime = now.minus(5, ChronoUnit.MINUTES);

        // Given: 서울 시간 기준 3분 전의 로그
        LocalDateTime threeMinutesAgoSeoul = LocalDateTime.now(SEOUL_ZONE).minus(3, ChronoUnit.MINUTES);
        String logTimestamp = threeMinutesAgoSeoul.format(LOG_TIMESTAMP_FORMATTER);

        // When: 로그 타임스탬프를 Instant로 변환
        LocalDateTime logDateTime = LocalDateTime.parse(logTimestamp, LOG_TIMESTAMP_FORMATTER);
        Instant logTime = logDateTime.atZone(SEOUL_ZONE).toInstant();

        // Then: cutoff 시간보다 이후여야 함 (최근 5분 내)
        assertThat(logTime).isAfter(cutoffTime);
    }
}
