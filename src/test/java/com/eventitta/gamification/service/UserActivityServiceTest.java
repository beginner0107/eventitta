package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.domain.UserPoints;
import com.eventitta.gamification.repository.ActivityTypeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.gamification.repository.UserPointsRepository;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    private UserActivityRepository userActivityRepository;
    @Mock
    private ActivityTypeRepository activityTypeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BadgeService badgeService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private UserPointsRepository userPointsRepository;

    @InjectMocks
    private UserActivityService userActivityService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
            .thenReturn(new SimpleTransactionStatus());
    }

    private User createUser(Long id) {
        return User.builder()
            .id(id)
            .email("u" + id + "@test.com")
            .password("pw")
            .nickname("nick" + id)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
    }

    private ActivityType createActivityType(Long id, String code, int point) {
        return ActivityType.builder()
            .id(id)
            .code(code)
            .name("name")
            .defaultPoint(point)
            .build();
    }

    @Test
    @DisplayName("동일한 활동이 이미 존재하면 새로 저장하지 않는다")
    void givenExistingActivity_whenRecordActivity_thenSkip() {
        // given
        Long userId = 1L;
        String code = "CREATE_POST";
        Long targetId = 10L;
        User user = createUser(userId);
        ActivityType type = createActivityType(1L, code, 10);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityTypeRepository.findByCode(code)).thenReturn(Optional.of(type));
        when(userActivityRepository.saveAndFlush(any(UserActivity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        // when
        userActivityService.recordActivity(userId, code, targetId);

        // then
        verify(userActivityRepository).saveAndFlush(any(UserActivity.class));
        verify(userPointsRepository, never()).upsertAndAddPoints(anyLong(), anyInt());
        verify(badgeService, never()).checkAndAwardBadges(any(), any());
    }

    @Test
    @DisplayName("UserPoints가 없으면 생성한 후 배지 서비스가 호출된다")
    void givenNoPoints_whenRecordActivity_thenCreatePointsAndCallBadgeService() {
        // given
        Long userId = 1L;
        String code = "CREATE_POST";
        Long targetId = 10L;
        User user = createUser(userId);
        ActivityType type = createActivityType(1L, code, 5);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityTypeRepository.findByCode(code)).thenReturn(Optional.of(type));
        when(userActivityRepository.saveAndFlush(any(UserActivity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userPointsRepository.upsertAndAddPoints(userId, type.getDefaultPoint())).thenReturn(1);
        when(userPointsRepository.findByUserId(userId))
            .thenReturn(Optional.of(UserPoints.of(user)));

        // when
        userActivityService.recordActivity(userId, code, targetId);

        // then
        verify(userActivityRepository).saveAndFlush(any(UserActivity.class));
        verify(userPointsRepository).upsertAndAddPoints(userId, type.getDefaultPoint());
        verify(badgeService).checkAndAwardBadges(eq(user), any(UserPoints.class));
    }

    @Test
    @DisplayName("활동 취소 시 포인트가 감소하고 기록이 삭제된다")
    void givenActivity_whenRevokeActivity_thenPointsDecreasedAndActivityDeleted() {
        // given
        Long userId = 1L;
        String code = "CREATE_POST";
        Long targetId = 10L;
        User user = createUser(userId);
        ActivityType type = createActivityType(1L, code, 5);
        UserActivity activity = new UserActivity(user, type, targetId);
        UserPoints points = UserPoints.of(user);
        points.addPoints(20);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityTypeRepository.findByCode(code)).thenReturn(Optional.of(type));
        when(userActivityRepository.findByUserIdAndActivityType_IdAndTargetId(userId, type.getId(), targetId))
            .thenReturn(Optional.of(activity));
        when(userPointsRepository.findByUserId(userId)).thenReturn(Optional.of(points));

        // when
        userActivityService.revokeActivity(userId, code, targetId);

        // then
        assertThat(points.getPoints()).isEqualTo(15);
        verify(userActivityRepository).delete(activity);
    }
}
