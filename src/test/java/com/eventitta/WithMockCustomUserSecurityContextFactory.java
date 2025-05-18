package com.eventitta;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockCustomUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        Authentication auth =
            new UsernamePasswordAuthenticationToken(
                annotation.userId(),
                null,
                List.of()
            );
        ctx.setAuthentication(auth);
        return ctx;
    }
}
