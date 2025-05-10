package com.eventitta.auth.controller;

import com.eventitta.auth.dto.request.SignInRequest;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.dto.response.SignUpResponse;
import com.eventitta.auth.service.AuthService;
import com.eventitta.auth.service.SignUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "인증 API", description = "회원가입, 로그인 등의 인증 관련 API")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원가입", description = "회원 정보를 받아 회원가입을 수행합니다.")
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        var user = authService.signUp(request);
        return ResponseEntity.ok(SignUpResponse.of(user));
    }

    @Operation(summary = "로그인", description = "회원 정보를 받아 로그인을 수행합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", ref = "#/components/responses/TokensSetCookies"),
    })
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody SignInRequest request, HttpServletResponse response) {
        authService.login(request, response);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "토큰 재발급", description = "엑세스, 리프레시 토큰을 받아 토큰을 재발급합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", ref = "#/components/responses/TokensSetCookies"),
    })
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
        @CookieValue(name = "access_token", required = false) String accessToken,
        @CookieValue(name = "refresh_token", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        authService.refresh(accessToken, refreshToken, response);
        return ResponseEntity.ok().build();
    }
}

