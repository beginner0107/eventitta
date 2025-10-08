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

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userActivityRepository.save(any(UserActivity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        userActivityService.recordActivity(userId, USER_LOGIN, targetId);

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
        Long targetId = 10L;
        User user = createTestUser();
        user.earnPoints(20); // 초기 포인트 설정
        ActivityType type = ActivityType.DELETE_COMMENT;

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userActivityRepository.deleteByUserIdAndActivityTypeAndTargetId(userId, type, targetId))
            .thenReturn(1L);

        // when
        userActivityService.revokeActivity(userId, type, targetId);

        // then
        assertThat(user.getPoints()).isEqualTo(15);
        verify(userActivityRepository)
            .deleteByUserIdAndActivityTypeAndTargetId(userId, type, targetId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 활동 기록 시 예외가 발생한다")
    void givenNonExistentUser_whenRecordActivity_thenThrowException() {
        // given
        Long userId = 999L;
        Long targetId = 10L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userActivityService.recordActivity(userId, CREATE_POST, targetId))
            .isInstanceOf(RuntimeException.class);
    }
}
