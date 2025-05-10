package com.eventitta.auth.controller;

import com.eventitta.auth.dto.request.SignInRequest;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.dto.response.SignUpResponse;
import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.service.AuthService;
import com.eventitta.common.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "인증 API", description = "회원가입, 로그인 등의 인증 관련 API")
public class AuthController {
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "회원가입", description = "회원 정보를 받아 회원가입을 수행합니다.")
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest req) {
        var user = authService.signUp(req);
        return ResponseEntity.ok(SignUpResponse.of(user));
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody SignInRequest req, HttpServletResponse resp) {
        TokenResponse tokenResponse = authService.login(req.email(), req.password());
        CookieUtil.addTokenCookies(resp, tokenResponse, jwtTokenProvider);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
        @CookieValue(name = "access_token", required = false) String at,
        @CookieValue(name = "refresh_token", required = false) String rt,
        HttpServletResponse resp
    ) {
        TokenResponse tokens = authService.refreshTokens(at, rt);
        CookieUtil.addTokenCookies(resp, tokens, jwtTokenProvider);
        return ResponseEntity.ok().build();
    }
}

