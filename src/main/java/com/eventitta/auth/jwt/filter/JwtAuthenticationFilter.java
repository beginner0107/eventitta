package com.eventitta.auth.jwt.filter;

import com.eventitta.auth.domain.UserPrincipal;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.jwt.util.JwtTokenUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain
    ) throws ServletException, IOException {
        String accessToken = JwtTokenUtil.extractTokenFromRequest(request);

        if (accessToken != null) {
            try {
                tokenProvider.validateAccessToken(accessToken);
                Long userId = tokenProvider.getUserId(accessToken);
                String email = tokenProvider.getEmail(accessToken);
                String role = tokenProvider.getRole(accessToken);

                UserPrincipal userPrincipal = new UserPrincipal(userId, email, role);
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (AuthException ex) {
                SecurityContextHolder.clearContext();
                throw ex;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/");
    }
}
