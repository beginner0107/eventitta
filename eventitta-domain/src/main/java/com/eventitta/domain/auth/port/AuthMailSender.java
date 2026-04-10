package com.eventitta.domain.auth.port;

public interface AuthMailSender {

    void sendEmailVerification(String email, String rawToken);

    void sendPasswordReset(String email, String rawToken);
}
