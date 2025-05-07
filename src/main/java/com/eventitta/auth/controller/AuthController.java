package com.eventitta.auth.controller;

import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.dto.response.SignUpResponse;
import com.eventitta.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "인증 API", description = "회원가입, 로그인 등의 인증 관련 API")
public class AuthController {
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "회원가입", description = "회원 정보를 받아 회원가입을 수행합니다.")
    @PostMapping("/signup")
    public SignUpResponse signUp(@Valid @RequestBody SignUpRequest req) {
        var user = authService.signUp(req.toEntity(passwordEncoder));
        return SignUpResponse.of(user);
    }
}

