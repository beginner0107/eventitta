package com.eventitta.auth.service;

import com.eventitta.auth.dto.request.SignInRequest;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.common.util.CookieUtil;
import com.eventitta.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import static com.eventitta.auth.exception.AuthErrorCode.*;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final LoginService loginService;
    private final SignUpService signUpService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshService;
    private final JwtTokenProvider jwtTokenProvider;

    public User signUp(SignUpRequest signUpRequest) {
        return signUpService.register(signUpRequest);
    }

    public void login(SignInRequest request, HttpServletResponse response) {
        try {
            Long userId = loginService.authenticate(request.email(), request.password());
            TokenResponse tokens = tokenService.issueTokens(userId);
            CookieUtil.addTokenCookies(response, tokens, jwtTokenProvider);
        } catch (AuthenticationException e) {
            throw INVALID_CREDENTIALS.defaultException(e);
        }
    }

    public void refresh(String accessToken, String refreshToken, HttpServletResponse resp) {
        TokenResponse tokens = refreshService.refresh(accessToken, refreshToken);
        CookieUtil.addTokenCookies(resp, tokens, jwtTokenProvider);
    }
}
