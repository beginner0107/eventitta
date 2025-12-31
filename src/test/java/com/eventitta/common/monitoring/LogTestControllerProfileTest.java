package com.eventitta.common.monitoring;

import com.eventitta.common.config.redis.MockRedisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogTestController의 프로파일 기반 빈 활성화 테스트
 *
 * @Profile 어노테이션이 올바르게 동작하는지 검증합니다.
 * local과 test 프로파일에서만 활성화되므로, test 프로파일로 테스트합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MockRedisConfig.class)
class LogTestControllerProfileTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("test 프로파일에서 LogTestController 빈이 활성화된다")
    void logTestControllerBeanShouldExistInTestProfile() {
        // When & Then: test 프로파일에서 빈이 존재해야 함
        assertThat(context.containsBean("logTestController")).isTrue();

        LogTestController controller = context.getBean(LogTestController.class);
        assertThat(controller).isNotNull();
    }

    @Test
    @DisplayName("LogTestController는 @Profile({\"local\", \"test\"}) 어노테이션을 가진다")
    void logTestControllerShouldHaveProfileAnnotation() {
        // When: LogTestController 클래스의 @Profile 어노테이션 확인
        Profile profileAnnotation = LogTestController.class.getAnnotation(Profile.class);

        // Then: @Profile 어노테이션이 존재하고 local과 test를 포함해야 함
        assertThat(profileAnnotation).isNotNull();
        assertThat(profileAnnotation.value()).containsExactlyInAnyOrder("local", "test");
    }
}
