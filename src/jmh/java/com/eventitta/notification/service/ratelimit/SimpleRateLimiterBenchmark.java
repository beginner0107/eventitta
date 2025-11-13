package com.eventitta.notification.service.ratelimit;

import com.eventitta.notification.domain.AlertLevel;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class SimpleRateLimiterBenchmark {

    @Param({"1000", "5000", "10000", "20000", "50000"})
    int mapSize;

    private SimpleRateLimiter limiter;
    private Clock fixedClock;
    private String[] existingKeys;

    @State(Scope.Thread)
    public static class ThreadState {
        int keyIndex;
    }

    @Setup(Level.Trial)
    public void setup() {
        fixedClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        limiter = new SimpleRateLimiter(fixedClock);

        // 맵에 미리 다양한 키들을 추가하여 ConcurrentHashMap의 크기를 늘림
        for (int i = 0; i < mapSize; i++) {
            limiter.shouldSendAlert("EXISTING_KEY_" + i, AlertLevel.HIGH);
        }

        // 기존 키들을 배열로 저장하여 벤치마크에서 사용
        existingKeys = new String[Math.min(1000, mapSize)];
        for (int i = 0; i < existingKeys.length; i++) {
            existingKeys[i] = "EXISTING_KEY_" + i;
        }
    }

    @Benchmark
    public void existingKey_lookup(ThreadState ts, Blackhole bh) {
        String key = existingKeys[(ts.keyIndex++) % existingKeys.length];
        bh.consume(limiter.shouldSendAlert(key, AlertLevel.HIGH));
    }

    @Benchmark
    public void newKey_insert(ThreadState ts, Blackhole bh) {
        String key = "NEW_KEY_" + ts.keyIndex++;
        bh.consume(limiter.shouldSendAlert(key, AlertLevel.HIGH));
    }
}
