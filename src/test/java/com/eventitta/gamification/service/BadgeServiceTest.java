package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.Badge;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.EvaluationType;
import com.eventitta.gamification.domain.UserBadge;
import com.eventitta.gamification.dto.projection.ActivitySummaryProjection;
import com.eventitta.gamification.evaluator.BadgeRuleEvaluator;
import com.eventitta.gamification.repository.BadgeRuleRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.gamification.repository.UserBadgeRepository;
import com.eventitta.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.eventitta.gamification.domain.ActivityType.CREATE_COMMENT;
import static com.eventitta.gamification.domain.ActivityType.CREATE_POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private BadgeRuleRepository badgeRuleRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private UserActivityRepository userActivityRepository;

    @Mock
    private List<BadgeRuleEvaluator> evaluators;

    @Mock
    private BadgeRuleEvaluator mockEvaluator;

    @InjectMocks
    private BadgeService badgeService;

    @Test
    @DisplayName("활동 횟수가 임계치에 도달하면 배지가 발급된다")
    void givenThresholdMet_whenCheckAndAward_thenBadgeIssued() {
        // given
        User user = User.builder()
            .id(1L)
            .email("test@test.com")
            .nickname("testUser")
            .build();

        ActivityType activityType = CREATE_POST;

        Badge badge = Badge.builder()
            .id(1L)
            .name("첫 게시글")
            .description("첫 번째 게시글 작성")
            .build();

        BadgeRule rule = BadgeRule.builder()
            .id(1L)
            .badge(badge)
            .activityType(activityType)
            .evaluationType(EvaluationType.COUNT)
            .threshold(1)
            .enabled(true)
            .build();

        given(badgeRuleRepository.findAllEnabledWithBadge()).willReturn(List.of(rule));
        given(userActivityRepository.countActivitiesByUser(user.getId())).willReturn(Collections.emptyList());
        given(userBadgeRepository.findBadgeIdsByUserId(user.getId())).willReturn(Collections.emptySet());
        given(evaluators.iterator()).willReturn(List.of(mockEvaluator).iterator());
        given(mockEvaluator.supports(rule)).willReturn(true);
        given(mockEvaluator.isSatisfied(any(User.class), any(BadgeRule.class), anyMap(), anyMap())).willReturn(true);

        // when
        List<String> result = badgeService.checkAndAwardBadges(user);

        // then
        assertThat(result).containsExactly("첫 게시글");
        verify(userBadgeRepository).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("이미 발급된 배지는 중복 발급되지 않는다")
    void givenAlreadyAwardedBadge_whenCheckAndAward_thenNoDuplicateIssue() {
        // given
        User user = User.builder().id(1L).email("test@test.com").nickname("testUser").build();

        ActivityType activityType = CREATE_POST;

        Badge badge = Badge.builder()
            .id(1L)
            .name("첫 게시글")
            .description("첫 번째 게시글 작성")
            .build();

        BadgeRule rule = BadgeRule.builder()
            .id(1L)
            .badge(badge)
            .activityType(activityType)
            .evaluationType(EvaluationType.COUNT)
            .threshold(1)
            .enabled(true)
            .build();

        given(badgeRuleRepository.findAllEnabledWithBadge()).willReturn(List.of(rule));
        given(userActivityRepository.countActivitiesByUser(user.getId())).willReturn(Collections.emptyList());
        given(userBadgeRepository.findBadgeIdsByUserId(user.getId())).willReturn(Set.of(1L)); // 이미 획득한 뱃지

        // when
        List<String> result = badgeService.checkAndAwardBadges(user);

        // then
        assertThat(result).isEmpty();
        verify(userBadgeRepository, never()).save(any());
    }

    @Test
    @DisplayName("비활성화된 규칙은 평가되지 않는다")
    void givenDisabledRule_whenCheckAndAward_thenRuleIgnored() {
        // given
        User user = User.builder().id(1L).email("test@test.com").nickname("testUser").build();

        // findAllEnabledWithBadge()는 enabled=true인 규칙만 반환하므로 빈 리스트 반환
        given(badgeRuleRepository.findAllEnabledWithBadge()).willReturn(Collections.emptyList());
        given(userActivityRepository.countActivitiesByUser(user.getId())).willReturn(Collections.emptyList());
        given(userBadgeRepository.findBadgeIdsByUserId(user.getId())).willReturn(Collections.emptySet());

        // when
        List<String> result = badgeService.checkAndAwardBadges(user);

        // then
        assertThat(result).isEmpty();
        verify(userBadgeRepository, never()).save(any());
    }

    @Test
    @DisplayName("평가 조건을 만족하지 않으면 배지가 발급되지 않는다")
    void givenThresholdNotMet_whenCheckAndAward_thenNoBadgeIssued() {
        // given
        User user = User.builder().id(1L).email("test@test.com").nickname("testUser").build();

        Badge badge = Badge.builder()
            .id(1L)
            .name("고급 배지")
            .description("고급 활동 배지")
            .build();

        BadgeRule rule = BadgeRule.builder()
            .id(1L)
            .badge(badge)
            .activityType(CREATE_POST)
            .evaluationType(EvaluationType.COUNT)
            .threshold(10)
            .enabled(true)
            .build();

        given(badgeRuleRepository.findAllEnabledWithBadge()).willReturn(List.of(rule));
        given(userActivityRepository.countActivitiesByUser(user.getId())).willReturn(Collections.emptyList());
        given(userBadgeRepository.findBadgeIdsByUserId(user.getId())).willReturn(Collections.emptySet());
        given(evaluators.iterator()).willReturn(List.of(mockEvaluator).iterator());
        given(mockEvaluator.supports(rule)).willReturn(true);
        given(mockEvaluator.isSatisfied(any(User.class), any(BadgeRule.class), anyMap(), anyMap())).willReturn(false);

        // when
        List<String> result = badgeService.checkAndAwardBadges(user);

        // then
        assertThat(result).isEmpty();
        verify(userBadgeRepository, never()).save(any());
    }

    @Test
    @DisplayName("여러 배지 규칙 중 조건을 만족하는 배지만 발급된다")
    void givenMultipleRules_whenCheckAndAward_thenOnlyQualifiedBadgesIssued() {
        // given
        User user = User.builder().id(1L).email("test@test.com").nickname("testUser").build();

        Badge badge1 = Badge.builder().id(1L).name("첫 게시글").description("첫 번째 게시글 작성").build();
        Badge badge2 = Badge.builder().id(2L).name("첫 댓글").description("첫 번째 댓글 작성").build();

        BadgeRule rule1 = BadgeRule.builder().id(1L).badge(badge1).activityType(CREATE_POST).evaluationType(EvaluationType.COUNT).threshold(1).enabled(true).build();
        BadgeRule rule2 = BadgeRule.builder().id(2L).badge(badge2).activityType(CREATE_COMMENT).evaluationType(EvaluationType.COUNT).threshold(1).enabled(true).build();

        given(badgeRuleRepository.findAllEnabledWithBadge()).willReturn(List.of(rule1, rule2));
        given(userActivityRepository.countActivitiesByUser(user.getId())).willReturn(Collections.emptyList());
        given(userBadgeRepository.findBadgeIdsByUserId(user.getId())).willReturn(Collections.emptySet());
        given(evaluators.iterator()).willReturn(List.of(mockEvaluator).iterator());

        given(mockEvaluator.supports(any(BadgeRule.class))).willReturn(true);
        given(mockEvaluator.isSatisfied(any(User.class), any(BadgeRule.class), anyMap(), anyMap())).willReturn(true, false);

        // when
        List<String> result = badgeService.checkAndAwardBadges(user);

        // then
        assertThat(result).containsExactly("첫 게시글");
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }
}
