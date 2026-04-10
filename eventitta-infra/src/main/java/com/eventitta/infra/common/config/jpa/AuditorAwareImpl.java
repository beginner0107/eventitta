package com.eventitta.infra.common.config.jpa;

import com.eventitta.domain.common.util.SecurityUtil;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String ANONYMOUS = "anonymous";

    @Override
    public Optional<String> getCurrentAuditor() {
        String userName = SecurityUtil.getCurrentUserName();
        return ANONYMOUS.equals(userName) ? Optional.empty() : Optional.of(userName);
    }
}
