package com.eventitta.api.auth.jwt.filter;

import com.eventitta.api.auth.domain.UserPrincipal;
import com.eventitta.api.auth.jwt.ParsedAccessToken;
import com.eventitta.api.auth.jwt.JwtTokenProvider;
import com.eventitta.api.auth.jwt.util.JwtTokenUtil;
import com.eventitta.domain.auth.exception.AuthException;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.view.UserAuthView;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.eventitta.domain.auth.exception.AuthErrorCode.AUTH_SESSION_INVALIDATED;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserInternalFacade userInternalFacade;
    private final RefreshTokenRepository refreshTokenRepository;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String[] WHITELIST = {
        "/actuator/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/api/v1/auth/**",
        "/error",
        "/favicon.ico"
    };

    public JwtAuthenticationFilter(
        JwtTokenProvider tokenProvider,
        UserInternalFacade userInternalFacade,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.tokenProvider = tokenProvider;
        this.userInternalFacade = userInternalFacade;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain
    ) throws ServletException, IOException {
        String accessToken = JwtTokenUtil.extractTokenFromRequest(request);

        if (accessToken != null) {
            try {
                ParsedAccessToken parsedAccessToken = tokenProvider.parseAccessToken(accessToken);
                UserAuthView currentUser = userInternalFacade.findActiveUserById(parsedAccessToken.userId())
                    .orElseThrow(AUTH_SESSION_INVALIDATED::defaultException);
                if (currentUser.suspended() || !currentUser.emailVerified()) {
                    throw AUTH_SESSION_INVALIDATED.defaultException();
                }
                if (currentUser.authVersion() != parsedAccessToken.authVersion()) {
                    throw AUTH_SESSION_INVALIDATED.defaultException();
                }
                if (parsedAccessToken.sessionId() == null
                    || refreshTokenRepository.findByUserIdAndSessionId(parsedAccessToken.userId(), parsedAccessToken.sessionId()).isEmpty()) {
                    throw AUTH_SESSION_INVALIDATED.defaultException();
                }
                UserPrincipal userPrincipal = new UserPrincipal(currentUser, parsedAccessToken.sessionId());
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (AuthException ex) {
                SecurityContextHolder.clearContext();
                throw new BadCredentialsException(ex.getMessage(), ex);
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI();
        for (String pattern : WHITELIST) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }
}
