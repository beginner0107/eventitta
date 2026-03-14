package com.eventitta.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    @DisplayName("role prefix가 없으면 ROLE_ 접두어를 붙여 권한을 만든다")
    void constructor_addsRolePrefixWhenMissing() {
        UserPrincipal principal = new UserPrincipal(1L, "user@test.com", "USER");

        assertThat(principal.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("이미 ROLE_ prefix가 있으면 그대로 유지한다")
    void constructor_keepsExistingRolePrefix() {
        UserPrincipal principal = new UserPrincipal(1L, "admin@test.com", "ROLE_ADMIN");

        assertThat(principal.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN");
    }
}
