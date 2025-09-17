package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityTypeRepository extends JpaRepository<ActivityType, Long> {
    Optional<ActivityType> findByCode(String code);
}
