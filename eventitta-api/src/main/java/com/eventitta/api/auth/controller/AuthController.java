package com.eventitta.api.auth.controller;

import com.eventitta.api.auth.controller.request.ActionTokenRequest;
import com.eventitta.api.auth.controller.request.EmailRequest;
import com.eventitta.api.auth.controller.request.PasswordResetConfirmRequest;
import com.eventitta.api.auth.controller.request.SignInRequest;
import com.eventitta.api.auth.controller.request.SignUpRequest;
import com.eventitta.api.auth.controller.request.SocialAuthorizeRequest;
import com.eventitta.api.auth.controller.request.SocialLoginRequest;
import com.eventitta.api.auth.controller.response.SignUpResponse;
import com.eventitta.api.auth.controller.response.SocialAuthorizeResponse;
import com.eventitta.api.auth.mapper.AuthMapper;
import com.eventitta.api.auth.web.ClientSessionMetadataResolver;
import com.eventitta.api.auth.web.CookieManager;
import com.eventitta.api.auth.web.KakaoAuthorizationSupport;
import com.eventitta.domain.auth.service.AuthService;
import com.eventitta.domain.auth.service.dto.ClientSessionMetadata;
import com.eventitta.domain.auth.service.dto.LogoutCommand;
import com.eventitta.domain.auth.service.dto.RefreshCommand;
import com.eventitta.domain.auth.service.dto.TokenResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.eventitta.api.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.api.auth.constants.AuthConstants.OAUTH_STATE;
import static com.eventitta.api.auth.constants.AuthConstants.REFRESH_TOKEN;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "인증 API", description = "회원가입, 로그인 등의 인증 관련 API")
public class AuthController {
    private final AuthService authService;
    private final AuthMapper authMapper;
    private final CookieManager cookieManager;
    private final KakaoAuthorizationSupport kakaoAuthorizationSupport;
    private final ClientSessionMetadataResolver clientSessionMetadataResolver;

    @Operation(summary = "회원가입", description = "회원 정보를 받아 회원가입을 수행합니다.")
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        var result = authService.signUp(authMapper.toSignUpCommand(request));
        return ResponseEntity.ok(authMapper.toSignUpResponse(result));
    }

    @Operation(summary = "로그인", description = "회원 정보를 받아 로그인을 수행합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", ref = "#/components/responses/TokensSetCookies"),
    })
    @PostMapping("/login")
    public ResponseEntity<Void> login(
        @Valid @RequestBody SignInRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse response
    ) {
        ClientSessionMetadata sessionMetadata = clientSessionMetadataResolver.resolve(servletRequest);
        TokenResult tokens = authService.login(authMapper.toSignInCommand(request, sessionMetadata));
        cookieManager.addTokenCookies(response, tokens);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "토큰 재발급", description = "엑세스, 리프레시 토큰을 받아 토큰을 재발급합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", ref = "#/components/responses/TokensSetCookies"),
    })
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
        @CookieValue(name = ACCESS_TOKEN, required = false) String accessToken,
        @CookieValue(name = REFRESH_TOKEN, required = false) String refreshToken,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        ClientSessionMetadata sessionMetadata = clientSessionMetadataResolver.resolve(request);
        RefreshCommand command = authMapper.toRefreshCommand(accessToken, refreshToken, sessionMetadata);
        TokenResult tokens = authService.refresh(command);
        cookieManager.addTokenCookies(response, tokens);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "로그아웃", description = "현재 로그인 세션을 종료하고 토큰을 무효화합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @Parameter(in = ParameterIn.COOKIE, name = ACCESS_TOKEN, description = "액세스 토큰")
        @CookieValue(name = ACCESS_TOKEN, required = false) String accessToken,
        @Parameter(in = ParameterIn.COOKIE, name = REFRESH_TOKEN, description = "리프레시 토큰")
        @CookieValue(name = REFRESH_TOKEN, required = false) String refreshToken,
        HttpServletResponse response
    ) {
        LogoutCommand command = authMapper.toLogoutCommand(accessToken, refreshToken);
        authService.logout(command);
        cookieManager.deleteTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "이메일 인증 메일 재발송", description = "미인증 로컬 계정에 이메일 인증 토큰을 다시 발송합니다.")
    @PostMapping("/email-verification/request")
    public ResponseEntity<Void> requestEmailVerification(
        @Valid @RequestBody EmailRequest request,
        HttpServletRequest servletRequest
    ) {
        ClientSessionMetadata sessionMetadata = clientSessionMetadataResolver.resolve(servletRequest);
        authService.requestEmailVerification(request.email(), sessionMetadata);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "이메일 인증 완료", description = "메일로 전달된 토큰으로 이메일 인증을 완료합니다.")
    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Void> confirmEmailVerification(@Valid @RequestBody ActionTokenRequest request) {
        authService.confirmEmailVerification(request.token());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 재설정 메일 발송", description = "로컬 계정에 비밀번호 재설정 토큰을 발송합니다.")
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
        @Valid @RequestBody EmailRequest request,
        HttpServletRequest servletRequest
    ) {
        ClientSessionMetadata sessionMetadata = clientSessionMetadataResolver.resolve(servletRequest);
        authService.requestPasswordReset(request.email(), sessionMetadata);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 재설정 완료", description = "메일로 전달된 토큰으로 비밀번호를 재설정합니다.")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "카카오 로그인 인가 URL 발급", description = "카카오 로그인 인가 URL을 생성하고 state 쿠키를 발급합니다.")
    @PostMapping("/social/kakao/authorize")
    public ResponseEntity<SocialAuthorizeResponse> authorizeKakao(
        @Valid @RequestBody SocialAuthorizeRequest request,
        HttpServletResponse response
    ) {
        String state = kakaoAuthorizationSupport.generateState();
        cookieManager.addShortLivedCookie(response, OAUTH_STATE, state);
        String authorizeUrl = kakaoAuthorizationSupport.buildAuthorizeUrl(request.redirectUri(), state);
        return ResponseEntity.ok(new SocialAuthorizeResponse(authorizeUrl));
    }

    @Operation(summary = "카카오 로그인", description = "카카오 인가 코드로 로그인하고 서비스 토큰 쿠키를 발급합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", ref = "#/components/responses/TokensSetCookies"),
    })
    @PostMapping("/social/kakao/login")
    public ResponseEntity<Void> loginWithKakao(
        @Valid @RequestBody SocialLoginRequest request,
        @CookieValue(name = OAUTH_STATE, required = false) String oauthState,
        HttpServletRequest servletRequest,
        HttpServletResponse response
    ) {
        try {
            kakaoAuthorizationSupport.validateState(oauthState, request.state());
            ClientSessionMetadata sessionMetadata = clientSessionMetadataResolver.resolve(servletRequest);
            TokenResult tokens = authService.loginWithKakao(authMapper.toKakaoLoginCommand(request, sessionMetadata));
            cookieManager.addTokenCookies(response, tokens);
            return ResponseEntity.ok().build();
        } finally {
            cookieManager.deleteCookie(response, OAUTH_STATE);
        }
    }
}
