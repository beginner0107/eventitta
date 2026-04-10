package com.eventitta.domain.gamification.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.gamification.domain.UserBadge;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.Set;

@NoRepositoryBean
public interface UserBadgeRepository extends BaseRepository<UserBadge, Long> {
    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

    @Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.userId = :userId")
    Set<Long> findBadgeIdsByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
