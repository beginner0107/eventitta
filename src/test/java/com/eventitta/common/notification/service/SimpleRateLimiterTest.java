package com.eventitta.common.notification.service;

import com.eventitta.common.notification.domain.AlertLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("알림 전송 빈도 제한 기능 - 과도한 알림을 방지하는 시스템")
class SimpleRateLimiterTest {

    private SimpleRateLimiter rateLimiter;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.systemDefault());
        rateLimiter = new SimpleRateLimiter(fixedClock);
    }

    @DisplayName("처음 발생하는 오류는 항상 알림을 보낸다")
    @Test
    void shouldAllowFirstAlert() {
        // given

        // when
        boolean shouldSend = rateLimiter.shouldSendAlert("TEST_ERROR", AlertLevel.CRITICAL);

        // then
        assertThat(shouldSend).isTrue();
    }

    @DisplayName("매우 위험한 오류는 10번까지 알림을 보낸 후 차단한다")
    @Test
    void shouldLimitCriticalAlerts() {
        // given
        String errorCode = "CRITICAL_ERROR";
        AlertLevel level = AlertLevel.CRITICAL;

        // when: 10번 연속 알림 요청
        for (int i = 1; i <= 10; i++) {
            boolean allowed = rateLimiter.shouldSendAlert(errorCode, level);

            // then: 1~10번째는 모두 허용
            assertThat(allowed).isTrue();
        }

        // when: 11번째 알림 요청
        boolean eleventh = rateLimiter.shouldSendAlert(errorCode, level);

        // then: 11번째부터는 차단됨
        assertThat(eleventh).isFalse();
    }

    @DisplayName("위험한 오류는 5번까지 알림을 보낸 후 차단한다")
    @Test
    void shouldLimitHighAlerts() {
        // given: 오류 코드와 심각도 설정
        String errorCode = "HIGH_ERROR";
        AlertLevel level = AlertLevel.HIGH;

        // when & then: 1~5번째는 허용, 6번째는 차단
        for (int i = 1; i <= 5; i++) {
            assertThat(rateLimiter.shouldSendAlert(errorCode, level))
                .isTrue();
        }
        // when: 6번째 요청
        boolean sixth = rateLimiter.shouldSendAlert(errorCode, level);
        // then
        assertThat(sixth).isFalse();
    }

    @DisplayName("보통 수준 오류는 2번까지 알림을 보낸 후 차단한다")
    @Test
    void shouldLimitMediumAlerts() {
        // given
        String errorCode = "MEDIUM_ERROR";
        AlertLevel level = AlertLevel.MEDIUM;

        // when & then: 1~2번째는 허용, 3번째는 차단
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
        // when: 3번째 요청
        boolean third = rateLimiter.shouldSendAlert(errorCode, level);
        // then
        assertThat(third).isFalse();
    }

    @DisplayName("정보성 알림은 1번만 보낸 후 차단한다")
    @Test
    void shouldLimitInfoAlerts() {
        // given
        String errorCode = "INFO_ERROR";
        AlertLevel level = AlertLevel.INFO;

        // when: 첫 번째 요청
        boolean first = rateLimiter.shouldSendAlert(errorCode, level);
        // then
        assertThat(first).isTrue();

        // when: 두 번째 요청
        boolean second = rateLimiter.shouldSendAlert(errorCode, level);
        // then
        assertThat(second).isFalse();
    }

    @DisplayName("서로 다른 종류의 오류는 각각 독립적으로 알림 제한을 적용한다")
    @Test
    void shouldLimitIndependentlyByErrorCode() {
        // given: 두 개의 오류 코드
        String errorCode1 = "ERROR_1";
        String errorCode2 = "ERROR_2";
        AlertLevel level = AlertLevel.INFO;

        // when & then: 각각 첫 요청은 허용
        assertThat(rateLimiter.shouldSendAlert(errorCode1, level)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode2, level)).isTrue();

        // when & then: 각각 두 번째 요청은 차단
        assertThat(rateLimiter.shouldSendAlert(errorCode1, level)).isFalse();
        assertThat(rateLimiter.shouldSendAlert(errorCode2, level)).isFalse();
    }

    @DisplayName("같은 오류라도 위험의 수준이 다르면 각각 독립적으로 알림 제한을 적용한다")
    @Test
    void shouldLimitIndependentlyByAlertLevel() {
        // given
        String errorCode = "TEST_ERROR";

        // when & then: CRITICAL 레벨은 첫 요청 허용
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.CRITICAL)).isTrue();

        // when & then: INFO 레벨은 별도 카운트로 첫 요청 허용
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.INFO)).isTrue();

        // when: INFO 레벨 두 번째 요청
        boolean infoSecond = rateLimiter.shouldSendAlert(errorCode, AlertLevel.INFO);
        // then
        assertThat(infoSecond).isFalse();

        // when & then: CRITICAL 레벨 카운트는 그대로 유지되어 허용
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.CRITICAL)).isTrue();
    }

    @DisplayName("일정 시간이 지나면 알림 제한이 초기화되어 다시 알림을 보낼 수 있다")
    @Test
    void shouldResetAfterTimeWindow() {
        // given: 제한 도달 상태
        String errorCode = "TEST_ERROR";
        AlertLevel level = AlertLevel.INFO;
        rateLimiter.shouldSendAlert(errorCode, level);
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();

        // when: 일정 시간이 지난 후 새로운 처리율 제한 객체 생성
        Clock newClock = Clock.fixed(
            Instant.parse("2023-01-01T00:06:00Z"),
            ZoneId.systemDefault()
        );
        rateLimiter = new SimpleRateLimiter(newClock);

        // then: 다시 알림 허용됨
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
    }

    @DisplayName("수동으로 초기화하면 모든 알림 제한이 해제된다")
    @Test
    void shouldResetAllLimits() {
        // given: 제한 도달 상태
        String errorCode = "TEST_ERROR";
        AlertLevel level = AlertLevel.INFO;
        rateLimiter.shouldSendAlert(errorCode, level);
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();

        // when: reset 호출
        rateLimiter.reset();

        // then: 모든 제한이 해제되어 허용
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
    }

    @DisplayName("여러 요청이 동시에 들어와도 정확한 수만큼만 알림을 허용한다")
    @Test
    void shouldBeThreadSafe() throws InterruptedException {
        // given: 동시에 요청할 갯수와 제한 설정
        String errorCode = "CONCURRENT_ERROR";
        AlertLevel level = AlertLevel.HIGH; // 최대 5회 허용
        int threadCount = 10;

        // when: ExecutorService와 CountDownLatch로 동시 처리
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                if (rateLimiter.shouldSendAlert(errorCode, level)) {
                    successCount.incrementAndGet();
                }
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();

        // then: 정확히 5개의 요청만 허용됨
        assertThat(successCount.get()).isEqualTo(5);
    }
}
