package com.eventitta.api.auth.domain;

import com.eventitta.domain.user.api.internal.view.UserAuthView;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {
    private static final String ROLE_PREFIX = "ROLE_";

    private final Long id;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final long authVersion;
    private final String sessionId;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(UserAuthView user) {
        this(user, null);
    }

    public UserPrincipal(UserAuthView user, String sessionId) {
        this.id = user.userId();
        this.email = user.email();
        this.password = user.encodedPassword();
        this.enabled = user.emailVerified() && !user.suspended();
        this.accountNonLocked = !user.suspended();
        this.authVersion = user.authVersion();
        this.sessionId = sessionId;
        this.authorities = List.of(new SimpleGrantedAuthority(normalizeRole(user.role())));
    }

    public UserPrincipal(Long id, String email, String role) {
        this(id, email, role, 0L, null);
    }

    public UserPrincipal(Long id, String email, String role, long authVersion) {
        this(id, email, role, authVersion, null);
    }

    public UserPrincipal(Long id, String email, String role, long authVersion, String sessionId) {
        this.id = id;
        this.email = email;
        this.password = null;
        this.enabled = true;
        this.accountNonLocked = true;
        this.authVersion = authVersion;
        this.sessionId = sessionId;
        this.authorities = List.of(new SimpleGrantedAuthority(normalizeRole(role)));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private String normalizeRole(String role) {
        if (role.startsWith(ROLE_PREFIX)) {
            return role;
        }
        return ROLE_PREFIX + role;
    }
}
