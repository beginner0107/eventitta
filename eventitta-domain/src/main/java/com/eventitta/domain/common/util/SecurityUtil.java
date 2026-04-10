package com.eventitta.domain.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Optional;

public final class SecurityUtil {

    private static final String ANONYMOUS = "anonymous";
    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String USER_INFO_FORMAT = "%s (ID: %d)";

    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isAnonymousAuthentication(auth)) {
            return Optional.empty();
        }
        return Optional.of(auth);
    }

    public static Optional<Object> getCurrentUser() {
        return getCurrentAuthentication()
            .map(Authentication::getPrincipal)
            .filter(principal -> !"anonymousUser".equals(String.valueOf(principal)));
    }

    public static String getCurrentUserInfo() {
        return getCurrentUser()
            .map(SecurityUtil::formatUserInfo)
            .or(() -> getCurrentAuthentication().map(Authentication::getName))
            .orElse(ANONYMOUS);
    }

    public static String getCurrentUserName() {
        return getCurrentAuthentication()
            .map(Authentication::getName)
            .orElse(ANONYMOUS);
    }

    private static boolean isAnonymousAuthentication(Authentication auth) {
        return auth == null ||
            !auth.isAuthenticated() ||
            ANONYMOUS_USER.equals(auth.getName());
    }

    private static String formatUserInfo(Object principal) {
        try {
            Method getEmail = principal.getClass().getMethod("getEmail");
            Method getId = principal.getClass().getMethod("getId");
            Object email = getEmail.invoke(principal);
            Object id = getId.invoke(principal);
            if (email instanceof String emailValue && id instanceof Number idValue) {
                return String.format(USER_INFO_FORMAT, emailValue, idValue.longValue());
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through to the principal string value below.
        }
        return String.valueOf(principal);
    }
}
