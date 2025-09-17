package com.eventitta.config;

import com.eventitta.common.config.AuditorAwareImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("현재 사용자 조회 기능")
class AuditorAwareImplTest {
    private final AuditorAwareImpl auditor = new AuditorAwareImpl();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 정보가 주어지지 않으면 사용자 식별 결과가 없다")
    void noAuth_returnsEmpty() {
        assertThat(auditor.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("익명 사용자(anonymousUser)로 인증된 경우 사용자 식별 결과가 없다")
    void anonymousUser_returnsEmpty() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("anonymousUser", null)
        );
        assertThat(auditor.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("인증된 사용자가 SecurityContext에 설정되면 사용자 이름을 반환한다")
    void authenticated_returnsName() {
        List<GrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken token =
            new UsernamePasswordAuthenticationToken("alice", null, auths);

        SecurityContextHolder.getContext().setAuthentication(token);

        assertThat(auditor.getCurrentAuditor()).hasValue("alice");
    }
}
