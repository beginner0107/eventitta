package com.eventitta.domain.auth.repository;

import com.eventitta.domain.auth.domain.AuthSecurityEvent;

public interface AuthSecurityEventRepository {

    AuthSecurityEvent save(AuthSecurityEvent authSecurityEvent);
}
