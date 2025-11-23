package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
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

import static com.eventitta.gamification.domain.ActivityType.CREATE_POST;
import static com.eventitta.gamification.domain.ActivityType.USER_LOGIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    private UserActivityRepository userActivityRepository;
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

    @Test
    @DisplayName("활동 기록 시 사용자 포인트가 증가하고 배지 서비스가 호출된다")
    void givenValidActivity_whenRecordActivity_thenPointsIncreasedAndBadgeServiceCalled() {
        // given
        Long userId = 1L;
        Long targetId = 10L;
        User user = createTestUser();
        int pointsToAdd = USER_LOGIN.getDefaultPoint();

        when(userActivityRepository.save(any(UserActivity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.incrementPoints(userId, pointsToAdd))
            .thenReturn(1); // 성공적으로 업데이트됨
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        userActivityService.recordActivity(userId, USER_LOGIN, targetId);

        // then
        verify(userActivityRepository).save(any(UserActivity.class));
        verify(userRepository).incrementPoints(userId, pointsToAdd);
        verify(badgeService).checkAndAwardBadges(user);
    }

    @Test
    @DisplayName("활동 취소 시 포인트가 감소하고 기록이 삭제된다")
    void givenActivity_whenRevokeActivity_thenPointsDecreasedAndActivityDeleted() {
        // given
        Long userId = 1L;
        Long targetId = 10L;
        ActivityType type = ActivityType.DELETE_COMMENT;
        int pointsToDeduct = type.getDefaultPoint();

        when(userActivityRepository.deleteByUserIdAndActivityTypeAndTargetId(userId, type, targetId))
            .thenReturn(1L);
        when(userRepository.decrementPoints(userId, pointsToDeduct))
            .thenReturn(1); // 성공적으로 차감됨

        // when
        userActivityService.revokeActivity(userId, type, targetId);

        // then
        verify(userActivityRepository)
            .deleteByUserIdAndActivityTypeAndTargetId(userId, type, targetId);
        verify(userRepository).decrementPoints(userId, pointsToDeduct);
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 활동 기록 시 예외가 발생한다")
    void givenNonExistentUser_whenRecordActivity_thenThrowException() {
        // given
        Long userId = 999L;
        Long targetId = 10L;
        int pointsToAdd = CREATE_POST.getDefaultPoint();

        when(userActivityRepository.save(any(UserActivity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.incrementPoints(userId, pointsToAdd))
            .thenReturn(0); // 사용자가 없어서 업데이트 실패

        // when & then
        assertThatThrownBy(() -> userActivityService.recordActivity(userId, CREATE_POST, targetId))
            .isInstanceOf(RuntimeException.class);
    }
}
