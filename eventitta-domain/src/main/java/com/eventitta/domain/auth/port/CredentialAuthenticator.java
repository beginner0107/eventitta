package com.eventitta.domain.auth.port;

public interface CredentialAuthenticator {

    Long authenticate(String email, String rawPassword);
}
