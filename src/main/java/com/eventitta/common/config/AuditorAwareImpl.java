package com.eventitta.common.config;

import com.eventitta.auth.jwt.constants.SecurityConstants;
import com.eventitta.common.util.SecurityUtil;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        String userName = SecurityUtil.getCurrentUserName();
        return SecurityConstants.ANONYMOUS.equals(userName) ? Optional.empty() : Optional.of(userName);
    }
}
