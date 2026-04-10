package com.eventitta.infra.auth.service;

import com.eventitta.domain.auth.port.AuthMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingAuthMailSender implements AuthMailSender {

    @Override
    public void sendEmailVerification(String email, String rawToken) {
        log.info("[AuthMail] email verification requested email={}, token={}", email, rawToken);
    }

    @Override
    public void sendPasswordReset(String email, String rawToken) {
        log.info("[AuthMail] password reset requested email={}, token={}", email, rawToken);
    }
}
