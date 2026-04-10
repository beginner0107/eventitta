package com.eventitta.api.auth.web;

import com.eventitta.domain.auth.service.dto.ClientSessionMetadata;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;

@Component
public class ClientSessionMetadataResolver {
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final Clock clock;

    public ClientSessionMetadataResolver(Clock clock) {
        this.clock = clock;
    }

    public ClientSessionMetadata resolve(HttpServletRequest request) {
        return new ClientSessionMetadata(
            resolveMaskedIp(request),
            normalizeUserAgent(request.getHeader("User-Agent")),
            clock.instant()
        );
    }

    private String resolveMaskedIp(HttpServletRequest request) {
        String candidate = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (!hasIpValue(candidate)) {
            candidate = request.getHeader("X-Real-IP");
        }
        if (!hasIpValue(candidate)) {
            candidate = request.getRemoteAddr();
        }
        if (!hasIpValue(candidate)) {
            return null;
        }
        return maskIp(candidate.trim());
    }

    private String firstForwardedIp(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }

        String[] parts = headerValue.split(",", 2);
        return parts[0].trim();
    }

    private boolean hasIpValue(String value) {
        return StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value.trim());
    }

    private String maskIp(String rawIp) {
        try {
            byte[] address = InetAddress.getByName(rawIp).getAddress();
            if (address.length == 4) {
                address[3] = 0;
                return InetAddress.getByAddress(address).getHostAddress() + "/24";
            }
            if (address.length == 16) {
                for (int index = 8; index < 16; index++) {
                    address[index] = 0;
                }
                return InetAddress.getByAddress(address).getHostAddress() + "/64";
            }
            return null;
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private String normalizeUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }

        String trimmed = userAgent.trim();
        if (trimmed.length() <= MAX_USER_AGENT_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_USER_AGENT_LENGTH);
    }
}
