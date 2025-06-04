package com.eventitta.event.repository;

import com.eventitta.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FestivalRepository extends JpaRepository<Event, Long> {

    Optional<Event> findBySourceAndTitleAndStartTime(
        String source,
        String title,
        LocalDateTime startTime
    );
}
