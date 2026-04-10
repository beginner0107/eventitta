package com.eventitta.domain.auth.service;

import org.springframework.security.crypto.keygen.KeyGenerators;

import java.util.Base64;

final class TokenKeyGenerator {

    private TokenKeyGenerator() {
    }

    static String newKey() {
        byte[] randomBytes = KeyGenerators.secureRandom(16).generateKey();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    static String newSecret() {
        byte[] randomBytes = KeyGenerators.secureRandom(32).generateKey();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
