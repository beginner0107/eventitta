package com.eventitta.domain.user.event;

import com.eventitta.domain.gamification.domain.RankingType;
import com.eventitta.domain.gamification.service.RankingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class UserDeletionRankingCleanupListenerTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @MockitoBean(name = "noopRankingService")
    private RankingService rankingService;

    @Test
    @DisplayName("사용자 탈퇴 이벤트가 커밋되면 포인트와 활동 랭킹에서 모두 제거한다")
    void handleUserDeleted_removesUserFromAllRankingsAfterCommit() {
        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new UserDeletedEvent(1L));
            return null;
        });

        // then
        verify(rankingService, timeout(1000)).removeUser(RankingType.POINTS, 1L);
        verify(rankingService, timeout(1000)).removeUser(RankingType.ACTIVITY_COUNT, 1L);
    }

    @Test
    @DisplayName("랭킹 제거 중 예외가 발생해도 커밋 후처리는 호출자를 실패시키지 않는다")
    void handleUserDeleted_rankingFailureDoesNotPropagate() {
        // given
        doThrow(new RuntimeException("redis failure"))
            .when(rankingService)
            .removeUser(org.mockito.ArgumentMatchers.any(RankingType.class), org.mockito.ArgumentMatchers.eq(2L));

        // when // then
        assertThatCode(() -> transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new UserDeletedEvent(2L));
            return null;
        })).doesNotThrowAnyException();

        verify(rankingService, timeout(1000)).removeUser(RankingType.POINTS, 2L);
        verify(rankingService, timeout(1000)).removeUser(RankingType.ACTIVITY_COUNT, 2L);
    }
}
