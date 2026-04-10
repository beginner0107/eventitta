package com.eventitta.domain.auth.domain;

import com.eventitta.domain.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "auth_identity",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_auth_identity_provider_user_id",
                columnNames = {"provider", "provider_user_id"}
            ),
            @UniqueConstraint(
                name = "uk_auth_identity_user_provider",
                columnNames = {"user_id", "provider"}
            )
        },
    indexes = {
        @Index(name = "idx_auth_identity_user_id", columnList = "user_id")
    }
)
public class AuthIdentity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    public static AuthIdentity create(
        Long userId,
        AuthProvider provider,
        String providerUserId,
        String providerEmail,
        boolean emailVerified
    ) {
        return AuthIdentity.builder()
            .userId(userId)
            .provider(provider)
            .providerUserId(providerUserId)
            .providerEmail(providerEmail)
            .emailVerified(emailVerified)
            .build();
    }
}
