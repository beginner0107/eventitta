package com.eventitta.domain.gamification.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.domain.gamification.api.internal.facade.GamificationInternalFacade;
import com.eventitta.domain.gamification.repository.UserGamificationStatsRepository;
import com.eventitta.domain.user.domain.Provider;
import com.eventitta.domain.user.domain.Role;
import com.eventitta.domain.user.domain.User;
import com.eventitta.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:gamification_facade_concurrency_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=VALUE,KEY,USER"
})
class GamificationFacadeConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private GamificationInternalFacade gamificationFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGamificationStatsRepository userGamificationStatsRepository;

    private ExecutorService executorService;
    private User user;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(8);
        long suffix = System.nanoTime();
        user = userRepository.save(User.builder()
            .email("concurrency-" + suffix + "@test.com")
            .password("password123")
            .nickname("concurrencyUser" + suffix)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    @DisplayName("동시에 여러 보상을 적립해도 stats 누락 없이 반영된다")
    void concurrentGrant_updatesStatsWithoutLoss() throws Exception {
        int threadCount = 5;
        int actionsPerThread = 3;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int thread = 0; thread < threadCount; thread++) {
            final int threadIndex = thread;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < actionsPerThread; i++) {
                        gamificationFacade.onPostCreated(user.getId(), (threadIndex * 100L) + i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(endLatch.await(10, TimeUnit.SECONDS)).isTrue();

        var totalStats = userGamificationStatsRepository.findById(user.getId()).orElseThrow();
        assertThat(totalStats.getTotalPoints()).isEqualTo(threadCount * actionsPerThread * 10);
        assertThat(totalStats.getTotalActivityCount()).isEqualTo(threadCount * actionsPerThread);
    }
}
