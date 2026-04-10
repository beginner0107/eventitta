package com.eventitta.api.auth.jwt;

import com.eventitta.api.auth.security.UserPrincipal;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.eventitta.domain.auth.exception.AuthErrorCode.NOT_FOUND_USER_EMAIL;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserInternalFacade userInternalFacade;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userInternalFacade.findActiveUserByEmail(email)
            .orElseThrow(NOT_FOUND_USER_EMAIL::defaultException);
        if (user.encodedPassword() == null || user.encodedPassword().isBlank()) {
            throw new UsernameNotFoundException(email);
        }
        return new UserPrincipal(user);
    }
}
