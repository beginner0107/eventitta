package com.eventitta.domain.auth.domain;

import com.eventitta.domain.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "refresh_tokens",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_refresh_tokens_token_key", columnNames = "token_key"),
        @UniqueConstraint(name = "uk_refresh_tokens_session_id", columnNames = "session_id")
    },
    indexes = {
        @Index(name = "idx_refresh_tokens_user_last_seen_at", columnList = "user_id, last_seen_at")
    }
)
public class RefreshToken extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "token_key", nullable = false, length = 100)
    private String tokenKey;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "issued_ip_masked", length = 64)
    private String issuedIpMasked;

    @Column(name = "issued_user_agent", length = 512)
    private String issuedUserAgent;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "last_seen_ip_masked", length = 64)
    private String lastSeenIpMasked;

    @Column(name = "last_seen_user_agent", length = 512)
    private String lastSeenUserAgent;

    public RefreshToken(
        Long userId,
        String sessionId,
        String tokenKey,
        String tokenHash,
        LocalDateTime expiresAt,
        String issuedIpMasked,
        String issuedUserAgent,
        LocalDateTime lastSeenAt,
        String lastSeenIpMasked,
        String lastSeenUserAgent
    ) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.tokenKey = tokenKey;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.issuedIpMasked = issuedIpMasked;
        this.issuedUserAgent = issuedUserAgent;
        this.lastSeenAt = lastSeenAt;
        this.lastSeenIpMasked = lastSeenIpMasked;
        this.lastSeenUserAgent = lastSeenUserAgent;
    }

    public static RefreshToken issue(
        Long userId,
        String sessionId,
        String tokenKey,
        String tokenHash,
        LocalDateTime expiresAt,
        LocalDateTime observedAt,
        String maskedIp,
        String userAgent
    ) {
        return new RefreshToken(
            userId,
            sessionId,
            tokenKey,
            tokenHash,
            expiresAt,
            maskedIp,
            userAgent,
            observedAt,
            maskedIp,
            userAgent
        );
    }

    public void rotate(
        String tokenKey,
        String tokenHash,
        LocalDateTime expiresAt,
        LocalDateTime observedAt,
        String maskedIp,
        String userAgent
    ) {
        this.tokenKey = tokenKey;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.lastSeenAt = observedAt;
        this.lastSeenIpMasked = maskedIp;
        this.lastSeenUserAgent = userAgent;
    }
}
