package com.eventitta.auth.service;

import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.service.dto.SignUpCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ActiveProfiles("test")
@SpringBootTest
class SignUpConcurrencyTest {

    @Autowired
    private SignUpService signUpService;

    @Test
    @DisplayName("동일 이메일로 동시 가입 시 하나만 성공하고 나머지는 CONFLICTED_EMAIL 예외가 발생한다")
    void concurrentSignUpWithSameEmail() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        SignUpCommand command = new SignUpCommand(
            "race@test.com", "password123!", "racer"
        );

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executor.submit(() -> {
                try {
                    latch.await(); // 모든 스레드가 동시에 출발
                    SignUpCommand cmd = new SignUpCommand(
                        "race@test.com", "password123!", "racer" + index
                    );
                    signUpService.register(cmd);
                    successCount.incrementAndGet();
                } catch (AuthException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    fail("예상하지 못한 예외: " + e.getClass().getSimpleName());
                }
            });
        }

        latch.countDown(); // 동시 출발
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threadCount - 1);
    }
}
