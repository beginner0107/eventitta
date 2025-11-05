package com.eventitta.notification.service.ratelimit;

import com.eventitta.notification.domain.AlertLevel;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class CacheBasedRateLimiterBenchmark {

    @Param({"1000", "5000", "10000", "20000", "50000"})
    int mapSize;

    private CacheBasedRateLimiter limiter;
    private String[] existingKeys;

    @State(Scope.Thread)
    public static class ThreadState {
        int keyIndex;
    }

    @Setup(Level.Trial)
    public void setup() {
        limiter = new CacheBasedRateLimiter();

        for (int i = 0; i < mapSize; i++) {
            limiter.shouldSendAlert("EXISTING_KEY_" + i, AlertLevel.INFO);
        }

        existingKeys = new String[1000];
        for (int i = 0; i < existingKeys.length; i++) {
            existingKeys[i] = "EXISTING_KEY_" + i;
        }
    }

    @Benchmark
    public void existingKey_lookup(ThreadState ts, Blackhole bh) {
        String key = existingKeys[(ts.keyIndex++) % existingKeys.length];
        bh.consume(limiter.shouldSendAlert(key, AlertLevel.INFO));
    }

    @Benchmark
    public void newKey_insert(ThreadState ts, Blackhole bh) {
        String key = "NEW_KEY_" + (ts.keyIndex++ & 0xFFFF);
        bh.consume(limiter.shouldSendAlert(key, AlertLevel.INFO));
    }
}
