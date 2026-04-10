package com.eventitta.api.auth.security.credential;

import com.eventitta.api.auth.security.principal.UserPrincipal;
import com.eventitta.domain.auth.exception.AuthErrorCode;
import com.eventitta.domain.auth.port.CredentialAuthenticator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import static com.eventitta.domain.auth.exception.AuthErrorCode.INVALID_CREDENTIALS;

@Component
@RequiredArgsConstructor
public class AuthenticationManagerCredentialAuthenticator implements CredentialAuthenticator {
    private final AuthenticationManager authenticationManager;

    @Override
    public Long authenticate(String email, String rawPassword) {
        try {
            var token = new UsernamePasswordAuthenticationToken(email, rawPassword);
            var authentication = authenticationManager.authenticate(token);
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        } catch (DisabledException ex) {
            throw AuthErrorCode.EMAIL_VERIFICATION_REQUIRED.defaultException(ex);
        } catch (LockedException ex) {
            throw AuthErrorCode.ACCOUNT_SUSPENDED.defaultException(ex);
        } catch (AuthenticationException ex) {
            throw INVALID_CREDENTIALS.defaultException(ex);
        }
    }
}
