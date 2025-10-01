package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.repository.ActivityTypeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private UserActivityService userActivityService;

    private User createTestUser() {
        return User.builder()
            .id(1L)
            .email("test@test.com")
            .password("pw")
            .nickname("testuser")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
    }

    private ActivityType createActivityType(String code, int point) {
        return ActivityType.builder()
            .id(1L)
            .code(code)
            .name("name")
            .defaultPoint(point)
            .build();
    }

    @Test
    @DisplayName("활동 기록 시 사용자 포인트가 증가하고 배지 서비스가 호출된다")
    void givenValidActivity_whenRecordActivity_thenPointsIncreasedAndBadgeServiceCalled() {
        // given
        Long userId = 1L;
        String code = "CREATE_POST";
        Long targetId = 10L;
        User user = createTestUser();
        ActivityType type = createActivityType(code, 5);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityTypeRepository.findByCode(code)).thenReturn(Optional.of(type));
        when(userActivityRepository.save(any(UserActivity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        userActivityService.recordActivity(userId, code, targetId);

        // then
        assertThat(user.getPoints()).isEqualTo(5);
        verify(userActivityRepository).save(any(UserActivity.class));
        verify(badgeService).checkAndAwardBadges(user);
    }

    @Test
    @DisplayName("활동 취소 시 포인트가 감소하고 기록이 삭제된다")
    void givenActivity_whenRevokeActivity_thenPointsDecreasedAndActivityDeleted() {
        // given
        Long userId = 1L;
        String code = "CREATE_POST";
        Long targetId = 10L;
        User user = createTestUser();
        user.earnPoints(20); // 초기 포인트 설정
        ActivityType type = createActivityType(code, 5);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityTypeRepository.findByCode(code)).thenReturn(Optional.of(type));
        when(userActivityRepository.deleteByUserIdAndActivityType_IdAndTargetId(userId, type.getId(), targetId))
            .thenReturn(1L);

        // when
        userActivityService.revokeActivity(userId, code, targetId);

        // then
        assertThat(user.getPoints()).isEqualTo(15);
        verify(userActivityRepository)
            .deleteByUserIdAndActivityType_IdAndTargetId(userId, type.getId(), targetId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 활동 기록 시 예외가 발생한다")
    void givenNonExistentUser_whenRecordActivity_thenThrowException() {
        // given
        Long userId = 999L;
        String code = "CREATE_POST";
        Long targetId = 10L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userActivityService.recordActivity(userId, code, targetId))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("존재하지 않는 활동 타입으로 기록 시 예외가 발생한다")
    void givenNonExistentActivityType_whenRecordActivity_thenThrowException() {
        // given
        Long userId = 1L;
        String code = "INVALID_CODE";
        Long targetId = 10L;
        User user = createTestUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityTypeRepository.findByCode(code)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userActivityService.recordActivity(userId, code, targetId))
            .isInstanceOf(RuntimeException.class);
    }
}
