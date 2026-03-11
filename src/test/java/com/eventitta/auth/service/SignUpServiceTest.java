package com.eventitta.auth.service;

import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.service.dto.SignUpCommand;
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

import static com.eventitta.auth.exception.AuthErrorCode.CONFLICTED_EMAIL;
import static com.eventitta.auth.exception.AuthErrorCode.CONFLICTED_NICKNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignUpServiceTest {

    @InjectMocks
    private SignUpService signUpService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("중복되지 않은 이메일과 닉네임이면 비밀번호를 인코딩해 사용자를 저장한다.")
    void register_success() {
        // given
        SignUpCommand command = new SignUpCommand(
            "test@example.com",
            "rawPassword",
            "tester"
        );

        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(userRepository.existsByNickname(command.nickname())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("encoded-password");

        User savedUser = mock(User.class);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // when
        User result = signUpService.register(command);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User user = userCaptor.getValue();
        assertThat(user.getEmail()).isEqualTo(command.email());
        assertThat(user.getNickname()).isEqualTo(command.nickname());
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(result).isEqualTo(savedUser);
    }

    @Test
    @DisplayName("이메일이 이미 존재하면 중복된 이메일이라는 예외를 던진다.")
    void register_fail_when_email_conflicted() {
        // given
        SignUpCommand command = new SignUpCommand(
            "test@example.com",
            "rawPassword",
            "tester"
        );

        when(userRepository.existsByEmail(command.email())).thenReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> signUpService.register(command));

        // then
        assertThat(thrown).isInstanceOf(AuthException.class);
        assertThat(((AuthException) thrown).getErrorCode()).isEqualTo(CONFLICTED_EMAIL);

        verify(userRepository, never()).existsByNickname(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("이메일은 중복되지 않았지만 닉네임이 이미 존재하면 중복된 닉네임이라는 예외를 던진다.")
    void register_fail_when_nickname_conflicted() {
        // given
        SignUpCommand command = new SignUpCommand(
            "test@example.com",
            "rawPassword",
            "tester"
        );

        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(userRepository.existsByNickname(command.nickname())).thenReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> signUpService.register(command));

        // then
        assertThat(thrown).isInstanceOf(AuthException.class);
        assertThat(((AuthException) thrown).getErrorCode()).isEqualTo(CONFLICTED_NICKNAME);

        verify(userRepository, never()).save(any(User.class));
    }
}
