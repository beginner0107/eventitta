package com.eventitta.domain.auth.service;

import com.eventitta.domain.auth.config.AuthRateLimitProperties;
import com.eventitta.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthAttemptGuard {

    private final AuthRateLimitProperties properties;
    private final Clock clock;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public void assertAllowed(String flow, String identifier, String clientIpMasked) {
        String key = composeKey(flow, identifier, clientIpMasked);
        AttemptState state = attempts.get(key);
        if (state == null) {
            return;
        }
        Instant now = clock.instant();
        if (state.lockedUntil != null && state.lockedUntil.isAfter(now)) {
            throw AuthErrorCode.AUTH_RATE_LIMITED.defaultException();
        }
        if (state.windowStartedAt.plusSeconds(properties.getWindowSeconds()).isBefore(now)) {
            attempts.remove(key);
        }
    }

    public void recordSuccess(String flow, String identifier, String clientIpMasked) {
        attempts.remove(composeKey(flow, identifier, clientIpMasked));
    }

    public void recordFailure(String flow, String identifier, String clientIpMasked) {
        String key = composeKey(flow, identifier, clientIpMasked);
        attempts.compute(key, (ignored, current) -> {
            Instant now = clock.instant();
            AttemptState state = current;
            if (state == null || state.windowStartedAt.plusSeconds(properties.getWindowSeconds()).isBefore(now)) {
                state = new AttemptState(now, 0, null);
            }

            int failures = state.failures + 1;
            Instant lockedUntil = failures >= maxFailuresFor(flow)
                ? now.plusSeconds(properties.getLockSeconds())
                : state.lockedUntil;
            return new AttemptState(state.windowStartedAt, failures, lockedUntil);
        });
    }

    private int maxFailuresFor(String flow) {
        return switch (flow) {
            case "login" -> properties.getLoginMaxFailures();
            case "social-login" -> properties.getSocialLoginMaxFailures();
            case "refresh" -> properties.getRefreshMaxFailures();
            default -> properties.getLoginMaxFailures();
        };
    }

    private String composeKey(String flow, String identifier, String clientIpMasked) {
        String normalizedIdentifier = StringUtils.hasText(identifier) ? identifier.trim().toLowerCase() : "anonymous";
        String normalizedIp = StringUtils.hasText(clientIpMasked) ? clientIpMasked : "unknown-ip";
        return flow + ":" + normalizedIdentifier + ":" + normalizedIp;
    }

    private record AttemptState(Instant windowStartedAt, int failures, Instant lockedUntil) {
    }
}
