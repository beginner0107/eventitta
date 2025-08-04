package com.eventitta.festival.repository;

import com.eventitta.festival.domain.CulturalEvent;
import com.eventitta.festival.domain.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CulturalEventRepository extends JpaRepository<CulturalEvent, Long> {
    Optional<CulturalEvent> findByExternalIdAndDataSource(String externalId, DataSource dataSource);
}
