package com.eventitta.gamification.repository;

import com.eventitta.gamification.domain.UserPoints;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPointsRepository extends JpaRepository<UserPoints, Long> {
}
