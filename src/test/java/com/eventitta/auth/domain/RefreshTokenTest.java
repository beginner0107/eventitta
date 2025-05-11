package com.eventitta.auth.domain;

import com.eventitta.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("리프레시 토큰 엔티티 단위 테스트")
class RefreshTokenTest {
    @Test
    @DisplayName("리프레시 토큰 갱신 시 해시와 만료 시각이 새 값으로 변경된다")
    void updateToken_updatesHashAndExpiry() {
        // given
        User user = User.builder().id(10L).build();
        RefreshToken rt = new RefreshToken(user, "oldHash", Instant.now().plusSeconds(3600));

        String newHash = "newHash";
        Instant newExpiry = Instant.now().plusSeconds(7200);

        // when
        rt.updateToken(newHash, newExpiry);

        // then
        assertThat(rt.getTokenHash()).isEqualTo(newHash);
        LocalDateTime expected = LocalDateTime.ofInstant(newExpiry, ZoneId.systemDefault());
        assertThat(rt.getExpiresAt()).isEqualTo(expected);
    }
}
