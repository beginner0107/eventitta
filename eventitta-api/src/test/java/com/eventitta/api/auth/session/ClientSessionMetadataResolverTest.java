package com.eventitta.api.auth.session;

import com.eventitta.domain.auth.dto.ClientSessionMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ClientSessionMetadataResolverTest {

    private final ClientSessionMetadataResolver resolver = new ClientSessionMetadataResolver(
        Clock.fixed(Instant.parse("2026-03-27T10:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    @DisplayName("X-Forwarded-For 첫 번째 주소를 우선 사용하고 IPv4 는 /24 로 마스킹한다")
    void resolve_prefersForwardedForAndMasksIpv4() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.42, 10.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.setRemoteAddr("198.51.100.20");

        ClientSessionMetadata metadata = resolver.resolve(request);

        assertThat(metadata.maskedIp()).isEqualTo("203.0.113.0/24");
        assertThat(metadata.userAgent()).isEqualTo("Mozilla/5.0");
        assertThat(metadata.observedAt()).isEqualTo(Instant.parse("2026-03-27T10:00:00Z"));
    }

    @Test
    @DisplayName("IPv6 는 /64 로 마스킹하고 user agent 는 512자로 자른다")
    void resolve_masksIpv6AndTruncatesUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "2001:db8:85a3:0:0:8a2e:370:7334");
        request.addHeader("User-Agent", "a".repeat(600));

        ClientSessionMetadata metadata = resolver.resolve(request);

        assertThat(metadata.maskedIp()).isEqualTo("2001:db8:85a3:0:0:0:0:0/64");
        assertThat(metadata.userAgent()).hasSize(512);
    }
}
