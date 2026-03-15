package com.eventitta.user.domain;

import com.eventitta.user.exception.UserException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.eventitta.user.exception.UserErrorCode.INSUFFICIENT_POINTS;
import static com.eventitta.user.exception.UserErrorCode.INVALID_POINTS_AMOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    @DisplayName("delete()는 식별 정보를 변조하고 개인정보를 초기화한다")
    void delete_clearsPersonalDataAndMutatesIdentifiers() {
        User user = User.builder()
            .id(42L)
            .email("user@test.com")
            .password("encodedPassword")
            .nickname("originalNick")
            .profilePictureUrl("https://example.com/pic.jpg")
            .selfIntro("Hello")
            .interests(List.of("music"))
            .address("Seoul")
            .latitude(new BigDecimal("37.123456"))
            .longitude(new BigDecimal("127.123456"))
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();

        user.delete();

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getNickname()).startsWith("__deleted_user_42_");
        assertThat(user.getEmail()).startsWith("__deleted_user_42_");
        assertThat(user.getEmail()).endsWith("@deleted.local");
        assertThat(user.getPassword()).isNull();
        assertThat(user.getProfilePictureUrl()).isNull();
        assertThat(user.getSelfIntro()).isNull();
        assertThat(user.getInterests()).isNull();
        assertThat(user.getAddress()).isNull();
        assertThat(user.getLatitude()).isNull();
        assertThat(user.getLongitude()).isNull();
        assertThat(user.getPoints()).isZero();
    }

    @Test
    @DisplayName("삭제된 이메일 형식은 Jakarta @Email 검증을 통과한다")
    void delete_generatedEmailPassesJakartaEmailValidation() {
        User user = User.builder()
            .id(1L)
            .email("user@test.com")
            .password("encodedPassword")
            .nickname("nick")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();

        user.delete();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            var violations = validator.validateProperty(user, "email");
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("earnPoints 는 양수 포인트를 누적한다")
    void earnPoints_addsPositiveAmount() {
        User user = createUser(10);

        user.earnPoints(5);

        assertThat(user.getPoints()).isEqualTo(15);
    }

    @Test
    @DisplayName("earnPoints 는 0 이하 포인트를 거부한다")
    void earnPoints_nonPositiveAmount_throws() {
        User user = createUser(10);

        assertThatThrownBy(() -> user.earnPoints(0))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(INVALID_POINTS_AMOUNT);
    }

    @Test
    @DisplayName("deductPoints 는 보유 포인트가 충분하면 차감한다")
    void deductPoints_subtractsWhenEnoughPoints() {
        User user = createUser(10);

        user.deductPoints(4);

        assertThat(user.getPoints()).isEqualTo(6);
    }

    @Test
    @DisplayName("deductPoints 는 0 이하 포인트를 거부한다")
    void deductPoints_nonPositiveAmount_throws() {
        User user = createUser(10);

        assertThatThrownBy(() -> user.deductPoints(-1))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(INVALID_POINTS_AMOUNT);
    }

    @Test
    @DisplayName("deductPoints 는 보유 포인트보다 큰 차감을 거부한다")
    void deductPoints_insufficientPoints_throws() {
        User user = createUser(3);

        assertThatThrownBy(() -> user.deductPoints(4))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(INSUFFICIENT_POINTS);
    }

    private User createUser(int points) {
        return User.builder()
            .id(1L)
            .email("user@test.com")
            .password("encodedPassword")
            .nickname("nick")
            .points(points)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .deleted(false)
            .build();
    }
}
