package com.eventitta.infra.auth.repository;

import com.eventitta.domain.auth.domain.AuthSecurityEvent;
import com.eventitta.domain.auth.repository.AuthSecurityEventRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuthSecurityEventRepository extends JpaRepository<AuthSecurityEvent, Long>, AuthSecurityEventRepository {
}
