package com.eventitta.domain.gamification.service;

import com.eventitta.domain.gamification.domain.Badge;
import com.eventitta.domain.gamification.domain.BadgeRule;
import com.eventitta.domain.gamification.domain.EvaluationType;
import com.eventitta.domain.gamification.domain.RewardActionType;
import com.eventitta.domain.gamification.repository.BadgeRuleRepository;
import com.eventitta.domain.gamification.repository.UserBadgeRepository;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.view.UserProfileView;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.eventitta.domain.gamification.domain.ActivityType.CREATE_COMMENT;
import static com.eventitta.domain.gamification.domain.ActivityType.CREATE_POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private BadgeRuleRepository badgeRuleRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private UserInternalFacade userInternalFacade;

    @InjectMocks
    private BadgeService badgeService;

    @Test
    @DisplayName("활동 횟수가 임계치에 도달하면 배지가 발급된다")
    void givenThresholdMet_whenCheckAndAward_thenBadgeIssued() {
        Badge badge = Badge.builder()
            .id(1L)
            .name("첫 게시글")
            .description("첫 번째 게시글 작성")
            .build();

        BadgeRule rule = BadgeRule.builder()
            .id(1L)
            .badge(badge)
            .activityType(CREATE_POST)
            .evaluationType(EvaluationType.COUNT)
            .threshold(1)
            .enabled(true)
            .build();

        given(userInternalFacade.findUserProfile(1L)).willReturn(Optional.of(activeProfile(1L)));
        given(badgeRuleRepository.findEnabledWithBadgeByActivityType(CREATE_POST)).willReturn(List.of(rule));
        given(userBadgeRepository.findBadgeIdsByUserId(1L)).willReturn(Collections.emptySet());

        List<String> result = badgeService.checkAndAwardBadges(1L, RewardActionType.CREATE_POST, 1L, 10);

        assertThat(result).containsExactly("첫 게시글");
        verify(userBadgeRepository).save(any());
    }

    @Test
    @DisplayName("이미 발급된 배지는 중복 발급되지 않는다")
    void givenAlreadyAwardedBadge_whenCheckAndAward_thenNoDuplicateIssue() {
        Badge badge = Badge.builder().id(1L).name("첫 게시글").description("첫 번째 게시글 작성").build();
        BadgeRule rule = BadgeRule.builder()
            .id(1L)
            .badge(badge)
            .activityType(CREATE_POST)
            .evaluationType(EvaluationType.COUNT)
            .threshold(1)
            .enabled(true)
            .build();

        given(userInternalFacade.findUserProfile(1L)).willReturn(Optional.of(activeProfile(1L)));
        given(badgeRuleRepository.findEnabledWithBadgeByActivityType(CREATE_POST)).willReturn(List.of(rule));
        given(userBadgeRepository.findBadgeIdsByUserId(1L)).willReturn(Set.of(1L));

        List<String> result = badgeService.checkAndAwardBadges(1L, RewardActionType.CREATE_POST, 1L, 10);

        assertThat(result).isEmpty();
        verify(userBadgeRepository, never()).save(any());
    }

    @Test
    @DisplayName("포인트 기준 규칙은 누적 포인트를 사용한다")
    void givenPointsRule_whenThresholdMet_thenBadgeIssued() {
        Badge badge = Badge.builder().id(2L).name("프로 댓글러").description("댓글 포인트 달성").build();
        BadgeRule rule = BadgeRule.builder()
            .id(2L)
            .badge(badge)
            .activityType(CREATE_COMMENT)
            .evaluationType(EvaluationType.POINTS)
            .threshold(15)
            .enabled(true)
            .build();

        given(userInternalFacade.findUserProfile(1L)).willReturn(Optional.of(activeProfile(1L)));
        given(badgeRuleRepository.findEnabledWithBadgeByActivityType(CREATE_COMMENT)).willReturn(List.of(rule));
        given(userBadgeRepository.findBadgeIdsByUserId(1L)).willReturn(Set.of());

        List<String> result = badgeService.checkAndAwardBadges(1L, RewardActionType.CREATE_COMMENT, 3L, 15);

        assertThat(result).containsExactly("프로 댓글러");
        verify(userBadgeRepository).save(any());
    }

    @Test
    @DisplayName("활성 사용자가 없으면 뱃지 처리를 건너뛴다")
    void givenDeletedUser_whenCheckAndAward_thenSkip() {
        given(userInternalFacade.findUserProfile(1L)).willReturn(Optional.empty());

        List<String> result = badgeService.checkAndAwardBadges(1L, RewardActionType.CREATE_POST, 1L, 10);

        assertThat(result).isEmpty();
        verify(badgeRuleRepository, never()).findEnabledWithBadgeByActivityType(any());
    }

    private UserProfileView activeProfile(Long userId) {
        return new UserProfileView(userId, "testUser", null, null, false);
    }
}
