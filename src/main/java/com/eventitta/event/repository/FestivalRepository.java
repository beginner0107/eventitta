package com.eventitta.event.repository;

import com.eventitta.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalRepository extends JpaRepository<Event, Long> {
}
