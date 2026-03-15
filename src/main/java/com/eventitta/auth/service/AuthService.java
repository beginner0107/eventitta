package com.eventitta.auth.service;

import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.mapper.AuthMapper;
import com.eventitta.auth.service.dto.*;
import com.eventitta.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.REFRESH_TOKEN;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final LoginService loginService;
    private final SignUpService signUpService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshService;
    private final CookieManager cookieManager;
    private final AuthMapper authMapper;

    public SignUpResult signUp(SignUpCommand signUpCommand) {
        log.info("[회원가입 시작] email={}, nickname={}", signUpCommand.email(), signUpCommand.nickname());

        User user = signUpService.register(signUpCommand);

        log.info("[회원가입 완료] userId={}, email={}", user.getId(), user.getEmail());
        return authMapper.toSignUpResult(user);
    }

    public void login(SignInCommand command, HttpServletResponse response) {
        Long userId = loginService.authenticate(command.email(), command.password());
        TokenResult tokens = tokenService.issueTokens(userId);
        cookieManager.addTokenCookies(response, tokens);
    }

    public void refresh(RefreshCommand command, HttpServletResponse resp) {
        log.info("[토큰 갱신 시작]");

        TokenResult tokens = refreshService.refresh(command);
        cookieManager.addTokenCookies(resp, tokens);

        log.info("[토큰 갱신 완료]");
    }

    public void logout(LogoutCommand command, HttpServletResponse response) {
        log.info("[로그아웃 시작]");

        if (StringUtils.hasText(command.accessToken())
            && StringUtils.hasText(command.refreshToken())) {
            try {
                refreshService.invalidateByToken(command.accessToken(), command.refreshToken());
            } catch (AuthException e) {
                log.debug("[로그아웃] 토큰 검증 오류 무시", e);
            }
        }

        cookieManager.deleteCookie(response, ACCESS_TOKEN);
        cookieManager.deleteCookie(response, REFRESH_TOKEN);

        log.info("[로그아웃 완료]");
    }
}
