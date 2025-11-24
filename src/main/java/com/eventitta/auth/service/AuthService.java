package com.eventitta.auth.service;

import com.eventitta.auth.dto.request.SignInRequest;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.common.util.CookieUtil;
import com.eventitta.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.REFRESH_TOKEN;
import static com.eventitta.auth.exception.AuthErrorCode.INVALID_CREDENTIALS;

@Slf4j
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
        log.info("[회원가입 시작] email={}, nickname={}", signUpRequest.email(), signUpRequest.nickname());

        User user = signUpService.register(signUpRequest);

        log.info("[회원가입 완료] userId={}, email={}", user.getId(), user.getEmail());
        return user;
    }

    public void login(SignInRequest request, HttpServletResponse response) {
        log.info("[로그인 시도] email={}", request.email());

        try {
            Long userId = loginService.authenticate(request.email(), request.password());
            TokenResponse tokens = tokenService.issueTokens(userId);
            CookieUtil.addTokenCookies(response, tokens, jwtTokenProvider);

            log.info("[로그인 성공] userId={}, email={}", userId, request.email());
        } catch (AuthenticationException e) {
            log.warn("[로그인 실패] email={}, reason={}", request.email(), "잘못된 인증 정보");
            throw INVALID_CREDENTIALS.defaultException(e);
        }
    }

    public void refresh(String accessToken, String refreshToken, HttpServletResponse resp) {
        log.info("[토큰 갱신 시작]");

        TokenResponse tokens = refreshService.refresh(accessToken, refreshToken);
        CookieUtil.addTokenCookies(resp, tokens, jwtTokenProvider);

        log.info("[토큰 갱신 완료]");
    }

    public void logout(String accessToken, HttpServletResponse response) {
        log.info("[로그아웃 시작]");

        if (StringUtils.hasText(accessToken)) {
            try {
                refreshService.invalidateByAccessToken(accessToken);
            } catch (AuthException e) {
                log.debug("[로그아웃] 토큰 검증 오류 무시", e);
            }
        }
        CookieUtil.deleteCookie(response, ACCESS_TOKEN);
        CookieUtil.deleteCookie(response, REFRESH_TOKEN);

        log.info("[로그아웃 완료]");
    }
}
