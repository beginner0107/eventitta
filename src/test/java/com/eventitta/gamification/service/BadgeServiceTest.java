package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.Badge;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.UserBadge;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

        ActivityType activityType = ActivityType.builder()
            .id(1L)
            .code("CREATE_POST")
            .name("게시글 작성")
            .defaultPoint(10)
            .build();

        Badge badge = Badge.builder()
            .id(1L)
            .name("첫 게시글")
            .description("첫 번째 게시글 작성")
            .build();

        BadgeRule rule = BadgeRule.builder()
            .id(1L)
            .badge(badge)
            .activityType(activityType)
            .threshold(1)
            .enabled(true)
            .build();

        given(badgeRuleRepository.findAll()).willReturn(List.of(rule));
        given(userActivityRepository.countByUserIdAndActivityType_Id(user.getId(), activityType.getId())).willReturn(1L);
        given(userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())).willReturn(false);

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

        ActivityType activityType = ActivityType.builder()
            .id(1L)
            .code("CREATE_POST")
            .name("게시글 작성")
            .defaultPoint(10)
            .build();

        Badge badge = Badge.builder()
            .id(1L)
            .name("첫 게시글")
            .description("첫 번째 게시글 작성")
            .build();

        BadgeRule rule = BadgeRule.builder()
            .id(1L)
            .badge(badge)
            .activityType(activityType)
            .threshold(1)
            .enabled(true)
            .build();

        given(badgeRuleRepository.findAll()).willReturn(List.of(rule));
        given(userActivityRepository.countByUserIdAndActivityType_Id(user.getId(), activityType.getId())).willReturn(1L);
        given(userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())).willReturn(true);

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

        ActivityType activityType = ActivityType.builder()
            .id(1L)
            .code("CREATE_POST")
            .name("게시글 작성")
            .defaultPoint(10)
            .build();

        Badge badge = Badge.builder()
            .id(1L)
            .name("첫 게시글")
            .description("첫 번째 게시글 작성")
            .build();

        BadgeRule rule = BadgeRule.builder()
            .id(1L)
            .badge(badge)
            .activityType(activityType)
            .threshold(1)
            .enabled(false)
            .build();

        given(badgeRuleRepository.findAll()).willReturn(List.of(rule));

        // when
        List<String> result = badgeService.checkAndAwardBadges(user);

        // then
        assertThat(result).isEmpty();
        verify(userActivityRepository, never()).countByUserIdAndActivityType_Id(any(), any());
        verify(userBadgeRepository, never()).save(any());
    }

    @Test
    @DisplayName("평가 조건을 만족하지 않으면 배지가 발급되지 않는다")
    void givenThresholdNotMet_whenCheckAndAward_thenNoBadgeIssued() {
        // given
        User user = User.builder().id(1L).email("test@test.com").nickname("testUser").build();

        ActivityType activityType = ActivityType.builder()
            .id(1L)
            .code("CREATE_POST")
            .name("게시글 작성")
            .defaultPoint(10)
            .build();

        Badge badge = Badge.builder()
            .id(1L)
            .name("고급 배지")
            .description("고급 활동 배지")
            .build();

        BadgeRule rule = BadgeRule.builder()
            .id(1L)
            .badge(badge)
            .activityType(activityType)
            .threshold(10)
            .enabled(true)
            .build();

        given(badgeRuleRepository.findAll()).willReturn(List.of(rule));
        given(userActivityRepository.countByUserIdAndActivityType_Id(user.getId(), activityType.getId())).willReturn(5L);

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

        ActivityType postActivityType = ActivityType.builder().id(1L).code("CREATE_POST").name("게시글 작성").defaultPoint(10).build();
        ActivityType commentActivityType = ActivityType.builder().id(2L).code("CREATE_COMMENT").name("댓글 작성").defaultPoint(5).build();

        Badge badge1 = Badge.builder().id(1L).name("첫 게시글").description("첫 번째 게시글 작성").build();
        Badge badge2 = Badge.builder().id(2L).name("첫 댓글").description("첫 번째 댓글 작성").build();

        BadgeRule rule1 = BadgeRule.builder().id(1L).badge(badge1).activityType(postActivityType).threshold(1).enabled(true).build();
        BadgeRule rule2 = BadgeRule.builder().id(2L).badge(badge2).activityType(commentActivityType).threshold(1).enabled(true).build();

        given(badgeRuleRepository.findAll()).willReturn(List.of(rule1, rule2));

        // rule1: 조건 만족
        given(userActivityRepository.countByUserIdAndActivityType_Id(user.getId(), postActivityType.getId())).willReturn(1L);
        given(userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge1.getId())).willReturn(false);

        // rule2는 조건 미충족 → stubbing 생략

        // when
        List<String> result = badgeService.checkAndAwardBadges(user);

        // then
        assertThat(result).containsExactly("첫 게시글");
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }
}
