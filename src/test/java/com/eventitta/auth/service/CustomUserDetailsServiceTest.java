package com.eventitta.auth.service;

import com.eventitta.auth.domain.UserPrincipal;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static com.eventitta.auth.exception.AuthErrorCode.NOT_FOUND_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CustomUserDetailsService service;

    @Test
    @DisplayName("올바른 이메일을 입력하면 해당 사용자 정보를 찾아서 반환한다")
    void loadUserByUsername_returnsPrincipal() {
        User user = User.builder()
            .id(1L)
            .email("test@example.com")
            .password("pw")
            .nickname("nick")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
        given(userRepository.findActiveByEmail("test@example.com"))
            .willReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@example.com");

        assertThat(details).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) details).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일을 입력하면 사용자를 찾을 수 없다는 오류가 발생한다")
    void loadUserByUsername_notFound_throws() {
        given(userRepository.findActiveByEmail("none@none.com"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("none@none.com"))
            .isInstanceOf(AuthException.class)
            .hasMessage(NOT_FOUND_USER_EMAIL.defaultMessage());
    }
}
