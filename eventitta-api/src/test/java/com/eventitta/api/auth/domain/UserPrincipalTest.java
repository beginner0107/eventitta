package com.eventitta.api.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    @DisplayName("ROLE_ 접두어가 없는 역할명은 ROLE_ 권한으로 변환한다")
    void constructor_addsRolePrefixWhenMissing() {
        UserPrincipal principal = new UserPrincipal(1L, "user@test.com", "USER");

        assertThat(principal.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
    }
}
