package com.eventitta.auth.service;

import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.service.dto.LogoutCommand;
import com.eventitta.auth.service.dto.RefreshCommand;
import com.eventitta.auth.service.dto.SignInCommand;
import com.eventitta.auth.service.dto.SignUpCommand;
import com.eventitta.auth.service.dto.SignUpResult;
import com.eventitta.auth.service.dto.TokenResult;
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

    public SignUpResult signUp(SignUpCommand signUpCommand) {
        log.info("[회원가입 시작] email={}, nickname={}", signUpCommand.email(), signUpCommand.nickname());

        User user = signUpService.register(signUpCommand);

        log.info("[회원가입 완료] userId={}, email={}", user.getId(), user.getEmail());
        return SignUpResult.of(user);
    }

    public void login(SignInCommand command, HttpServletResponse response) {
        Long userId = loginService.authenticate(command.email(), command.password());
        TokenResult tokens = tokenService.issueTokens(userId);
        CookieUtil.addTokenCookies(response, tokens, jwtTokenProvider);
    }

    public void refresh(RefreshCommand command, HttpServletResponse resp) {
        log.info("[토큰 갱신 시작]");

        TokenResult tokens = refreshService.refresh(command);
        CookieUtil.addTokenCookies(resp, tokens, jwtTokenProvider);

        log.info("[토큰 갱신 완료]");
    }

    public void logout(LogoutCommand command, HttpServletResponse response) {
        log.info("[로그아웃 시작]");

        if (StringUtils.hasText(command.accessToken())) {
            try {
                refreshService.invalidateByAccessToken(command.accessToken());
            } catch (AuthException e) {
                log.debug("[로그아웃] 토큰 검증 오류 무시", e);
            }
        }
        CookieUtil.deleteCookie(response, ACCESS_TOKEN);
        CookieUtil.deleteCookie(response, REFRESH_TOKEN);

        log.info("[로그아웃 완료]");
    }
}
