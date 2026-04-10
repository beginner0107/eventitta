package com.eventitta.domain.auth.domain;

import com.eventitta.domain.common.domain.BaseTimeEntity;
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
    name = "auth_security_events",
    indexes = {
        @Index(name = "idx_auth_security_events_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_auth_security_events_type_created_at", columnList = "event_type, created_at")
    }
)
public class AuthSecurityEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuthSecurityEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_outcome", nullable = false, length = 30)
    private AuthSecurityEventOutcome outcome;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "identifier", length = 255)
    private String identifier;

    @Column(name = "client_ip_masked", length = 64)
    private String clientIpMasked;

    @Column(name = "client_user_agent", length = 512)
    private String clientUserAgent;

    @Column(name = "detail_message", length = 1000)
    private String detailMessage;

    public static AuthSecurityEvent create(
        Long userId,
        AuthSecurityEventType eventType,
        AuthSecurityEventOutcome outcome,
        String sessionId,
        String identifier,
        String clientIpMasked,
        String clientUserAgent,
        String detailMessage
    ) {
        return AuthSecurityEvent.builder()
            .userId(userId)
            .eventType(eventType)
            .outcome(outcome)
            .sessionId(sessionId)
            .identifier(identifier)
            .clientIpMasked(clientIpMasked)
            .clientUserAgent(clientUserAgent)
            .detailMessage(detailMessage)
            .build();
    }
}
