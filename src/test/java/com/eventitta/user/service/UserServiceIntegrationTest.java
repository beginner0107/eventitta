package com.eventitta.user.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.exception.UserErrorCode;
import com.eventitta.user.exception.UserException;
import com.eventitta.user.repository.UserRepository;
import com.eventitta.user.service.dto.ChangePasswordCommand;
import com.eventitta.user.service.dto.UpdateProfileCommand;
import com.eventitta.user.service.dto.UserProfileResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("사용자 서비스 통합 테스트")
@Transactional
class UserServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("프로필 조회에 성공하면 저장된 사용자 정보를 반환한다")
    void getProfile_returnsPersistedProfile() {
        // given
        User user = userRepository.saveAndFlush(createUser(
            "profile-success@test.com",
            "profileUser",
            "Password123!",
            0
        ));

        // when
        UserProfileResult result = userService.getProfile(user.getId());

        // then
        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(result.email()).isEqualTo("profile-success@test.com");
        assertThat(result.nickname()).isEqualTo("profileUser");
    }

    @Test
    @DisplayName("삭제된 사용자의 프로필 조회는 실패한다")
    void getProfile_deletedUser_throwsNotFound() {
        // given
        User user = userRepository.saveAndFlush(createUser(
            "profile-deleted@test.com",
            "deletedProfileUser",
            "Password123!",
            0
        ));
        user.delete();
        userRepository.flush();

        // when // then
        assertThatThrownBy(() -> userService.getProfile(user.getId()))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.NOT_FOUND_USER_ID);
    }

    @Test
    @DisplayName("프로필 수정에 성공하면 변경 사항이 저장된다")
    void updateProfile_persistsChanges() {
        // given
        User user = userRepository.saveAndFlush(createUser(
            "update-success@test.com",
            "beforeNick",
            "Password123!",
            0
        ));
        UpdateProfileCommand command = new UpdateProfileCommand(
            "afterNick",
            "https://example.com/profile.jpg",
            "updated intro",
            List.of("travel", "music"),
            "Busan",
            new BigDecimal("35.179554"),
            new BigDecimal("129.075642")
        );

        // when
        userService.updateProfile(user.getId(), command);

        // then
        User persistedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(persistedUser.getNickname()).isEqualTo("afterNick");
        assertThat(persistedUser.getProfilePictureUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(persistedUser.getSelfIntro()).isEqualTo("updated intro");
        assertThat(persistedUser.getInterests()).containsExactly("travel", "music");
        assertThat(persistedUser.getAddress()).isEqualTo("Busan");
        assertThat(persistedUser.getLatitude()).isEqualByComparingTo("35.179554");
        assertThat(persistedUser.getLongitude()).isEqualByComparingTo("129.075642");
    }

    @Test
    @DisplayName("닉네임이 그대로면 중복 검사 없이 프로필 수정에 성공한다")
    void updateProfile_sameNickname_doesNotConflict() {
        // given
        User user = userRepository.saveAndFlush(createUser(
            "same-nickname@test.com",
            "sameNick",
            "Password123!",
            0
        ));
        UpdateProfileCommand command = new UpdateProfileCommand(
            "sameNick",
            null,
            "still works",
            List.of("running"),
            "Seoul",
            new BigDecimal("37.566500"),
            new BigDecimal("126.978000")
        );

        // when
        userService.updateProfile(user.getId(), command);

        // then
        User persistedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(persistedUser.getNickname()).isEqualTo("sameNick");
        assertThat(persistedUser.getSelfIntro()).isEqualTo("still works");
        assertThat(persistedUser.getInterests()).containsExactly("running");
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 프로필 수정을 거부한다")
    void updateProfile_conflictedNickname_throws() {
        // given
        User user = userRepository.saveAndFlush(createUser(
            "conflict-owner@test.com",
            "ownerNick",
            "Password123!",
            0
        ));
        userRepository.saveAndFlush(createUser(
            "conflict-other@test.com",
            "duplicatedNick",
            "Password123!",
            0
        ));

        // when // then
        assertThatThrownBy(() -> userService.updateProfile(
            user.getId(),
            new UpdateProfileCommand(
                "duplicatedNick",
                null,
                null,
                List.of(),
                null,
                null,
                null
            )
        ))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.CONFLICTED_NICKNAME);
    }

    @Test
    @DisplayName("회원 탈퇴에 성공하면 사용자 정보를 익명화하고 리프레시 토큰을 삭제한다")
    void deleteUser_scrubsUserAndDeletesRefreshTokens() {
        // given
        User user = userRepository.saveAndFlush(createUser(
            "delete-success@test.com",
            "deleteUser",
            "Password123!",
            0
        ));
        refreshTokenRepository.saveAndFlush(new RefreshToken(
            user,
            "refresh-token-hash",
            LocalDateTime.now().plusDays(1)
        ));

        // when
        userService.deleteUser(user.getId());

        // then
        User deletedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deletedUser.isDeleted()).isTrue();
        assertThat(deletedUser.getNickname()).startsWith("__deleted_user_" + user.getId() + "_");
        assertThat(deletedUser.getEmail()).startsWith("__deleted_user_" + user.getId() + "_");
        assertThat(deletedUser.getEmail()).endsWith("@deleted.local");
        assertThat(deletedUser.getPassword()).isNull();
        assertThat(deletedUser.getProfilePictureUrl()).isNull();
        assertThat(deletedUser.getSelfIntro()).isNull();
        assertThat(deletedUser.getInterests()).isNull();
        assertThat(deletedUser.getAddress()).isNull();
        assertThat(deletedUser.getLatitude()).isNull();
        assertThat(deletedUser.getLongitude()).isNull();
        assertThat(refreshTokenRepository.findAllByUserId(user.getId())).isEmpty();
        assertThat(userRepository.findActiveById(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 회원 탈퇴할 수 없다")
    void deleteUser_missingUser_throws() {
        // when // then
        assertThatThrownBy(() -> userService.deleteUser(9999L))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.NOT_FOUND_USER_ID);
    }

    @Test
    @DisplayName("비밀번호 변경에 성공하면 새 비밀번호가 암호화되어 저장된다")
    void changePassword_updatesEncodedPassword() {
        // given
        User user = userRepository.saveAndFlush(createUser(
            "change-password@test.com",
            "passwordUser",
            "OldPassword123!",
            0
        ));

        // when
        userService.changePassword(user.getId(), new ChangePasswordCommand("OldPassword123!", "NewPassword123!"));

        // then
        User persistedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPassword123!", persistedUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("OldPassword123!", persistedUser.getPassword())).isFalse();
    }

    @Test
    @DisplayName("현재 비밀번호가 다르면 비밀번호 변경을 거부한다")
    void changePassword_wrongCurrentPassword_throws() {
        // given
        User user = userRepository.saveAndFlush(createUser(
            "change-password-fail@test.com",
            "passwordFailUser",
            "OldPassword123!",
            0
        ));

        // when // then
        assertThatThrownBy(() -> userService.changePassword(
            user.getId(),
            new ChangePasswordCommand("WrongPassword123!", "NewPassword123!")
        ))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.INVALID_CURRENT_PASSWORD);
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 비밀번호를 변경할 수 없다")
    void changePassword_missingUser_throws() {
        // when // then
        assertThatThrownBy(() -> userService.changePassword(
            9999L,
            new ChangePasswordCommand("OldPassword123!", "NewPassword123!")
        ))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.NOT_FOUND_USER_ID);
    }

    private User createUser(String email, String nickname, String rawPassword, int points) {
        return User.builder()
            .email(email)
            .password(passwordEncoder.encode(rawPassword))
            .nickname(nickname)
            .points(points)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .deleted(false)
            .build();
    }
}
