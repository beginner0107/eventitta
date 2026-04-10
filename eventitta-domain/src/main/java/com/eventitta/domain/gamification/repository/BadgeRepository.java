package com.eventitta.domain.gamification.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.gamification.domain.Badge;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface BadgeRepository extends BaseRepository<Badge, Long> {
    Optional<Badge> findByName(String name);
}
