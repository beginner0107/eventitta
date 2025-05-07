package com.eventitta.auth.controller;

import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.dto.response.SignUpResponse;
import com.eventitta.auth.service.AuthService;
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
public class AuthController {
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    public SignUpResponse signUp(@Valid @RequestBody SignUpRequest req) {
        var user = authService.signUp(req.toEntity(passwordEncoder));
        return SignUpResponse.of(user);
    }
}

