package com.eventitta.auth.service;

import com.eventitta.auth.domain.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService {
    private final AuthenticationManager authManager;

    public Long authenticate(String email, String rawPassword) {
        var token = new UsernamePasswordAuthenticationToken(email, rawPassword);
        var auth = authManager.authenticate(token);
        return ((UserPrincipal) auth.getPrincipal()).getId();
    }
}
