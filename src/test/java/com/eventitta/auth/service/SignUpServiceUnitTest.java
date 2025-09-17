package com.eventitta.auth.service;

import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원가입 단위 테스트")
class SignUpServiceUnitTest {

    @InjectMocks
    private SignUpService signUpService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @DisplayName("중복된 이메일인경우 예외를 전달한다.")
    @Test
    void givenDuplicatedEmail_whenSignUp_thenThrowsException() {
        // given
        SignUpRequest request = new SignUpRequest("dup@domain.com", "pw123!@#", "nick");
        given(userRepository.existsByEmail("dup@domain.com")).willReturn(true);

        // when
        AuthException e = assertThrows(AuthException.class, () -> signUpService.register(request));

        // then
        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.CONFLICTED_EMAIL);
    }

    @DisplayName("중복된 닉네임인경우 예외를 전달한다.")
    @Test
    void givenDuplicatedNickname_whenSignUp_thenThrowsException() {
        // given
        SignUpRequest request = new SignUpRequest("aaaaa@domain.com", "pw123!@#", "dupNickname");
        given(userRepository.existsByEmail("aaaaa@domain.com")).willReturn(false);
        given(userRepository.existsByNickname("dupNickname")).willReturn(true);

        // when
        AuthException e = assertThrows(AuthException.class, () -> signUpService.register(request));

        // then
        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.CONFLICTED_NICKNAME);
    }

    @Test
    @DisplayName("유효한 요청이면 비밀번호를 암호화하고, 회원 정보를 반환한다")
    void givenSignUpRequest_whenSignUp_thenReturnUser() {
        // given
        SignUpRequest request = new SignUpRequest("new@domain.com", "plainPw1!", "newUser");
        given(userRepository.existsByEmail("new@domain.com")).willReturn(false);
        given(userRepository.existsByNickname("newUser")).willReturn(false);
        given(passwordEncoder.encode("plainPw1!")).willReturn("encodedPw123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        User saved = User.builder()
            .id(100L)
            .email("new@domain.com")
            .password("encodedPw123")
            .nickname("newUser")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
        given(userRepository.save(captor.capture())).willReturn(saved);

        // when
        User result = signUpService.register(request);

        // then
        User toSave = captor.getValue();
        assertThat(toSave.getEmail()).isEqualTo("new@domain.com");
        assertThat(toSave.getPassword()).isEqualTo("encodedPw123");
        assertThat(toSave.getNickname()).isEqualTo("newUser");

        assertThat(result).isSameAs(saved);
    }
}
