package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.Badge;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.repository.*;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BadgeServiceTest {

    @Autowired
    BadgeService badgeService;
    @Autowired
    BadgeRepository badgeRepository;
    @Autowired
    BadgeRuleRepository badgeRuleRepository;
    @Autowired
    UserActivityRepository userActivityRepository;
    @Autowired
    UserBadgeRepository userBadgeRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ActivityTypeRepository activityTypeRepository;

    @Test
    @DisplayName("활동 횟수가 임계치에 도달하면 배지가 발급된다")
    void givenThresholdMet_whenCheckAndAward_thenBadgeIssued() {
        // given
        User user = createUser("badge@test.com", "nick");
        ActivityType activityType = createActivityType("CREATE_POST", "게시글 작성", 10);
        Badge badge = createBadge("3-post", "three posts");
        createBadgeRule(badge, "CREATE_POST", 3);

        for (long i = 0; i < 3; i++) {
            userActivityRepository.save(new UserActivity(user, activityType, i));
        }

        // when
        List<String> result = badgeService.checkAndAwardBadges(user);

        // then
        assertThat(result).containsExactly("3-post");
        assertThat(userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())).isTrue();
    }

    private User createUser(String email, String nickname) {
        return userRepository.save(
            User.builder()
                .email(email)
                .password("password")
                .nickname(nickname)
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .points(0)
                .build()
        );
    }

    private ActivityType createActivityType(String code, String name, int point) {
        return activityTypeRepository.save(
            ActivityType.builder()
                .code(code)
                .name(name)
                .defaultPoint(point)
                .build()
        );
    }

    private Badge createBadge(String name, String description) {
        return badgeRepository.save(
            Badge.builder()
                .name(name)
                .description(description)
                .iconUrl(null)
                .build()
        );
    }

    private void createBadgeRule(Badge badge, String activityCode, int threshold) {
        String conditionJson = String.format(
            "{\"type\": \"ACTIVITY_COUNT\", \"activityCode\": \"%s\", \"threshold\": %d}",
            activityCode, threshold
        );

        badgeRuleRepository.save(
            BadgeRule.builder()
                .badge(badge)
                .conditionJson(conditionJson)
                .enabled(true)
                .build()
        );
    }
}
