package com.eventitta.user.service;

import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.dto.ChangePasswordRequest;
import com.eventitta.user.dto.UpdateProfileRequest;
import com.eventitta.user.dto.UserProfileResponse;
import com.eventitta.user.exception.UserErrorCode;
import com.eventitta.user.exception.UserException;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    private User createUser(Long id) {
        return User.builder()
            .id(id)
            .email("u" + id + "@test.com")
            .password("encoded")
            .nickname("nick" + id)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
    }

    @Test
    @DisplayName("사용자 ID로 프로필을 조회하면 사용자 프로필 정보를 반환한다")
    void getProfile_returnsDto() {
        User user = createUser(1L);
        given(userRepository.findActiveById(1L)).willReturn(Optional.of(user));

        UserProfileResponse resp = userService.getProfile(1L);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.email()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 프로필 조회 시 예외가 발생한다")
    void getProfile_userNotFound_throws() {
        given(userRepository.findActiveById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(1L))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.NOT_FOUND_USER_ID);
    }

    @Test
    @DisplayName("닉네임이 이미 존재하면 프로필 수정에 실패한다")
    void updateProfile_conflictNickname_throws() {
        User user = createUser(1L);
        UpdateProfileRequest req = new UpdateProfileRequest(
            "newNick",
            null,
            null,
            List.of(),
            null,
            null,
            null
        );
        given(userRepository.findActiveById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("newNick")).willReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(1L, req))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.CONFLICTED_NICKNAME);
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 비밀번호 변경에 실패한다")
    void changePassword_wrongCurrent_throws() {
        User user = createUser(1L);
        ChangePasswordRequest req = new ChangePasswordRequest("cur", "newPassword1!");
        given(userRepository.findActiveById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("cur", user.getPassword())).willReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, req))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.INVALID_CURRENT_PASSWORD);
    }

    @Test
    @DisplayName("정상적인 요청으로 비밀번호 변경에 성공한다")
    void changePassword_success() {
        User user = createUser(1L);
        ChangePasswordRequest req = new ChangePasswordRequest("cur", "newPassword1!");
        given(userRepository.findActiveById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("cur", user.getPassword())).willReturn(true);
        given(passwordEncoder.encode("newPassword1!")).willReturn("encodedNew");

        userService.changePassword(1L, req);

        verify(userRepository).findActiveById(1L);
        assertThat(user.getPassword()).isEqualTo("encodedNew");
    }
}
