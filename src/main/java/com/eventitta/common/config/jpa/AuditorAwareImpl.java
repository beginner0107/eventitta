package com.eventitta.common.config.jpa;

import com.eventitta.common.util.SecurityUtil;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

import static com.eventitta.auth.constants.AuthConstants.ANONYMOUS;

public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        String userName = SecurityUtil.getCurrentUserName();
        return ANONYMOUS.equals(userName) ? Optional.empty() : Optional.of(userName);
    }
}
