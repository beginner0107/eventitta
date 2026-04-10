package com.eventitta.domain.auth.domain;

import com.eventitta.domain.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "auth_action_tokens",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_auth_action_tokens_token_key", columnNames = "token_key")
    },
    indexes = {
        @Index(name = "idx_auth_action_tokens_user_purpose", columnList = "user_id, token_purpose"),
        @Index(name = "idx_auth_action_tokens_expires_at", columnList = "expires_at")
    }
)
public class AuthActionToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_purpose", nullable = false, length = 50)
    private AuthActionTokenPurpose purpose;

    @Column(name = "token_key", nullable = false, length = 100)
    private String tokenKey;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "target_email", length = 255)
    private String targetEmail;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "requested_ip_masked", length = 64)
    private String requestedIpMasked;

    @Column(name = "requested_user_agent", length = 512)
    private String requestedUserAgent;

    public static AuthActionToken issue(
        Long userId,
        AuthActionTokenPurpose purpose,
        String tokenKey,
        String tokenHash,
        String targetEmail,
        LocalDateTime expiresAt,
        String requestedIpMasked,
        String requestedUserAgent
    ) {
        return AuthActionToken.builder()
            .userId(userId)
            .purpose(purpose)
            .tokenKey(tokenKey)
            .tokenHash(tokenHash)
            .targetEmail(targetEmail)
            .expiresAt(expiresAt)
            .requestedIpMasked(requestedIpMasked)
            .requestedUserAgent(requestedUserAgent)
            .build();
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
}
