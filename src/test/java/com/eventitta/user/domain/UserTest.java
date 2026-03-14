package com.eventitta.user.domain;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
