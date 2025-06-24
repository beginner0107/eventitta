package com.eventitta.gamification.repository;

import com.eventitta.common.config.QuerydslConfig;
import com.eventitta.gamification.domain.Badge;
import com.eventitta.gamification.domain.UserBadge;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(QuerydslConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "com.eventitta")
class UserBadgeRepositoryTest {

    @Autowired
    UserBadgeRepository userBadgeRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    BadgeRepository badgeRepository;

    @Test
    @DisplayName("같은 배지를 두 번 발급하려 하면 예외가 발생한다")
    void saveDuplicateUserBadge_throwsException() {
        User user = userRepository.save(
            User.builder()
                .email("dup@test.com")
                .password("encoded123")
                .nickname("nick")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .build()
        );

        Badge badge;
        try {
            var ctor = Badge.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            badge = ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(badge, "name", "test-badge");
        ReflectionTestUtils.setField(badge, "description", "desc");
        Badge savedBadge = badgeRepository.save(badge);

        userBadgeRepository.saveAndFlush(new UserBadge(user, savedBadge));

        assertThatThrownBy(() ->
            userBadgeRepository.saveAndFlush(new UserBadge(user, savedBadge))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
