package com.eventitta.domain.auth.service;

import com.eventitta.domain.auth.domain.AuthSecurityEvent;
import com.eventitta.domain.auth.domain.AuthSecurityEventOutcome;
import com.eventitta.domain.auth.domain.AuthSecurityEventType;
import com.eventitta.domain.auth.repository.AuthSecurityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthSecurityEventService {

    private final AuthSecurityEventRepository authSecurityEventRepository;

    public void record(
        Long userId,
        AuthSecurityEventType eventType,
        AuthSecurityEventOutcome outcome,
        String sessionId,
        String identifier,
        String clientIpMasked,
        String clientUserAgent,
        String detailMessage
    ) {
        authSecurityEventRepository.save(AuthSecurityEvent.create(
            userId,
            eventType,
            outcome,
            sessionId,
            identifier,
            clientIpMasked,
            clientUserAgent,
            detailMessage
        ));
        log.info(
            "[AuthSecurityEvent] type={}, outcome={}, userId={}, sessionId={}, identifier={}, ip={}",
            eventType,
            outcome,
            userId,
            sessionId,
            identifier,
            clientIpMasked
        );
    }
}
