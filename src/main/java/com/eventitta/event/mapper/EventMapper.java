package com.eventitta.event.mapper;

import com.eventitta.event.domain.Event;

public interface EventMapper<T> {
    Event toEntity(T source, String origin);
}
