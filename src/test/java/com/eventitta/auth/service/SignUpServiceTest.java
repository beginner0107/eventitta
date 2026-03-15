package com.eventitta.auth.service;

import com.eventitta.auth.service.dto.SignUpCommand;
import com.eventitta.user.exception.UserErrorCode;
import com.eventitta.user.exception.UserException;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.catchThrowable;
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
    @DisplayName("저장 시 이메일 unique constraint 위반이 발생하면 중복 이메일 예외로 변환한다.")
    void register_fail_when_email_unique_violation_occurs_on_save() {
        SignUpCommand command = new SignUpCommand(
            "test@example.com",
            "rawPassword",
            "tester"
        );

        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(userRepository.existsByNickname(command.nickname())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any()))
            .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'uk_users_email'"));

        Throwable thrown = catchThrowable(() -> signUpService.register(command));

        assertThat(thrown).isInstanceOf(UserException.class);
        assertThat(((UserException) thrown).getErrorCode()).isEqualTo(UserErrorCode.CONFLICTED_EMAIL);
    }

    @Test
    @DisplayName("저장 시 닉네임 unique constraint 위반이 발생하면 중복 닉네임 예외로 변환한다.")
    void register_fail_when_nickname_unique_violation_occurs_on_save() {
        SignUpCommand command = new SignUpCommand(
            "test@example.com",
            "rawPassword",
            "tester"
        );

        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(userRepository.existsByNickname(command.nickname())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any()))
            .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'uk_users_nickname'"));

        Throwable thrown = catchThrowable(() -> signUpService.register(command));

        assertThat(thrown).isInstanceOf(UserException.class);
        assertThat(((UserException) thrown).getErrorCode()).isEqualTo(UserErrorCode.CONFLICTED_NICKNAME);
    }
}
