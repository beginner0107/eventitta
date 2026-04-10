package com.eventitta.api.common.logging;

import com.eventitta.domain.common.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves the actor identifier for the current request, for use in
 * exception and audit logging.
 *
 * <p>Returns the SecurityContext principal when authenticated, otherwise
 * falls back to the client IP so every log line has a non-empty actor field.
 * This is a logging helper — it does not authenticate and must not be used
 * for authorization decisions.
 */
@Component
public class RequestActorResolver {

    private static final String ANONYMOUS = "anonymous";
    private static final String[] CLIENT_IP_HEADERS = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP"
    };

    public String resolveActor(HttpServletRequest request) {
        String userInfo = SecurityUtil.getCurrentUserInfo();
        if (ANONYMOUS.equals(userInfo)) {
            return extractClientIp(request);
        }
        return userInfo;
    }

    private static String extractClientIp(HttpServletRequest request) {
        for (String header : CLIENT_IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
