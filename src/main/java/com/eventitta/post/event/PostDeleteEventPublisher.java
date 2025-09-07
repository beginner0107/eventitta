package com.eventitta.post.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostDeleteEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(PostDeletedEvent postDeleteEvent) {
        eventPublisher.publishEvent(postDeleteEvent);
    }
}
