package com.eventitta.notification.service.ratelimit;

import com.eventitta.notification.domain.AlertLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

/**
 * RedisRateLimiter의 Fallback 동작을 검증하는 단위 테스트
 *
 * <p>Redis 장애 상황에서 로컬 캐시로 Fallback하는지 확인합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class RedisRateLimiterFallbackTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CacheBasedRateLimiter fallbackLimiter;
    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        fallbackLimiter = new CacheBasedRateLimiter();
        rateLimiter = new RedisRateLimiter(stringRedisTemplate, fallbackLimiter);

        // stringRedisTemplate.opsForValue() 모킹
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Redis 연결 실패 시 로컬 캐시로 Fallback")
    void shouldFallbackToLocalCacheWhenRedisConnectionFails() {
        // given: Redis 연결 실패 시뮬레이션
        when(valueOperations.increment(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection refused"));

        String errorCode = "CONNECTION_FAIL_TEST";
        AlertLevel level = AlertLevel.HIGH; // 5회 제한

        // when & then: Fallback으로 여전히 동작해야 함
        for (int i = 0; i < 5; i++) {
            boolean allowed = rateLimiter.shouldSendAlert(errorCode, level);
            assertThat(allowed).isTrue();
        }

        // 6번째 요청은 Fallback에서 거부
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();

        // Redis 호출은 시도되었어야 함
        verify(valueOperations, atLeastOnce()).increment(anyString());
    }

    @Test
    @DisplayName("Redis 타임아웃 시 로컬 캐시로 Fallback")
    void shouldFallbackToLocalCacheWhenRedisTimeout() {
        // given: Redis 타임아웃 시뮬레이션
        when(valueOperations.increment(anyString()))
            .thenThrow(new RuntimeException("Command timed out"));

        String errorCode = "TIMEOUT_TEST";
        AlertLevel level = AlertLevel.CRITICAL;

        // when
        boolean result = rateLimiter.shouldSendAlert(errorCode, level);

        // then: Fallback으로 동작
        assertThat(result).isTrue();

        // Redis 시도 확인
        verify(valueOperations).increment(anyString());
    }

    @Test
    @DisplayName("Redis가 null을 반환할 때 Fallback")
    void shouldFallbackWhenRedisReturnsNull() {
        // given: Redis increment가 null 반환
        when(valueOperations.increment(anyString())).thenReturn(null);

        String errorCode = "NULL_RETURN_TEST";
        AlertLevel level = AlertLevel.INFO;

        // when
        boolean result = rateLimiter.shouldSendAlert(errorCode, level);

        // then: Fallback으로 허용
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Redis 정상 동작 후 장애 발생 시에도 Fallback 동작")
    void shouldFallbackAfterRedisFailure() {
        // given: 처음엔 정상 동작
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L);

        String errorCode = "INTERMITTENT_FAIL";
        AlertLevel level = AlertLevel.MEDIUM; // 2회 제한

        // when: 처음 2회는 Redis로 성공
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();

        // then: Redis가 갑자기 실패
        when(valueOperations.increment(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection lost"));

        // when: 3번째 요청은 Fallback으로 처리
        // Fallback은 독립적으로 카운트하므로 허용됨
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
    }

    @Test
    @DisplayName("reset() 호출 시 Redis 실패해도 Fallback은 초기화됨")
    void shouldResetFallbackEvenWhenRedisFails() {
        // given: Redis 실패 상태
        when(valueOperations.increment(anyString()))
            .thenThrow(new RedisConnectionFailureException("Redis down"));

        String errorCode = "RESET_TEST";
        AlertLevel level = AlertLevel.INFO; // 1회 제한

        // when: Fallback으로 1회 사용
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();

        // when: reset 호출
        rateLimiter.reset();

        // then: Fallback이 초기화되어 다시 허용
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
    }

    @Test
    @DisplayName("여러 종류의 예외에 대해 모두 Fallback 동작")
    void shouldFallbackOnVariousExceptions() {
        // given
        AlertLevel level = AlertLevel.HIGH;

        // when & then: RedisConnectionFailureException
        when(valueOperations.increment(contains("EXCEPTION_TEST_1")))
            .thenThrow(new RedisConnectionFailureException("Connection error"));
        assertThat(rateLimiter.shouldSendAlert("EXCEPTION_TEST_1", level)).isTrue();

        // when & then: RuntimeException
        when(valueOperations.increment(contains("EXCEPTION_TEST_2")))
            .thenThrow(new RuntimeException("Unknown error"));
        assertThat(rateLimiter.shouldSendAlert("EXCEPTION_TEST_2", level)).isTrue();

        // when & then: NullPointerException
        when(valueOperations.increment(contains("EXCEPTION_TEST_3")))
            .thenThrow(new NullPointerException("Null pointer"));
        assertThat(rateLimiter.shouldSendAlert("EXCEPTION_TEST_3", level)).isTrue();
    }
}
