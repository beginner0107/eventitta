package com.eventitta.common.util;

import com.eventitta.auth.constants.AuthConstants;
import com.eventitta.auth.domain.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtil {

    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isAnonymousAuthentication(auth)) {
            return Optional.empty();
        }
        return Optional.of(auth);
    }

    public static Optional<UserPrincipal> getCurrentUser() {
        return getCurrentAuthentication()
            .map(Authentication::getPrincipal)
            .filter(UserPrincipal.class::isInstance)
            .map(UserPrincipal.class::cast);
    }

    public static String getCurrentUserInfo() {
        return getCurrentUser()
            .map(SecurityUtil::formatUserInfo)
            .or(() -> getCurrentAuthentication().map(Authentication::getName))
            .orElse(AuthConstants.ANONYMOUS);
    }

    public static String getCurrentUserName() {
        return getCurrentAuthentication()
            .map(Authentication::getName)
            .orElse(AuthConstants.ANONYMOUS);
    }

    private static boolean isAnonymousAuthentication(Authentication auth) {
        return auth == null ||
            !auth.isAuthenticated() ||
            AuthConstants.ANONYMOUS_USER.equals(auth.getName());
    }

    private static String formatUserInfo(UserPrincipal user) {
        return String.format(AuthConstants.USER_INFO_FORMAT, user.getEmail(), user.getId());
    }
}
