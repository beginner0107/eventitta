package com.eventitta;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.eventitta.CustomPrincipal;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockCustomUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        CustomPrincipal principal = new CustomPrincipal(annotation.userId());
        Authentication auth = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of()
        );
        ctx.setAuthentication(auth);
        return ctx;
    }
}
