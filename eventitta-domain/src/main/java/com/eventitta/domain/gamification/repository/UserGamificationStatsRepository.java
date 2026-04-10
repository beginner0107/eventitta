package com.eventitta.domain.gamification.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.gamification.domain.UserGamificationStats;
import com.eventitta.domain.gamification.repository.projection.UserRankingProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;

@NoRepositoryBean
public interface UserGamificationStatsRepository
    extends BaseRepository<UserGamificationStats, Long>, UserGamificationStatsRepositoryCustom {

    @Query("""
        select s.userId as userId, u.nickname as nickname, u.profilePictureUrl as profilePictureUrl,
               s.totalPoints as score
        from UserGamificationStats s
        join User u on u.id = s.userId
        where u.deleted = false and s.totalPoints > 0
        order by s.totalPoints desc, s.userId asc
        """)
    List<UserRankingProjection> findTopByPoints(Pageable pageable);

    @Query("""
        select s.userId as userId, u.nickname as nickname, u.profilePictureUrl as profilePictureUrl,
               cast(s.totalActivityCount as integer) as score
        from UserGamificationStats s
        join User u on u.id = s.userId
        where u.deleted = false and s.totalActivityCount > 0
        order by s.totalActivityCount desc, s.userId asc
        """)
    List<UserRankingProjection> findTopByActivityCount(Pageable pageable);

    @Query("""
        select count(s)
        from UserGamificationStats s
        join User u on u.id = s.userId
        where u.deleted = false and s.totalPoints > :points
        """)
    long countUsersWithMorePointsThan(@Param("points") int points);

    @Query("""
        select count(s)
        from UserGamificationStats s
        join User u on u.id = s.userId
        where u.deleted = false and s.totalActivityCount > :activityCount
        """)
    long countUsersWithMoreActivityCountThan(@Param("activityCount") long activityCount);

    @Query("""
        select count(s)
        from UserGamificationStats s
        join User u on u.id = s.userId
        where u.deleted = false
          and (s.totalPoints > :points or (s.totalPoints = :points and s.userId < :userId))
        """)
    long countUsersRankedAheadByPoints(@Param("points") int points, @Param("userId") Long userId);

    @Query("""
        select count(s)
        from UserGamificationStats s
        join User u on u.id = s.userId
        where u.deleted = false
          and (s.totalActivityCount > :activityCount
               or (s.totalActivityCount = :activityCount and s.userId < :userId))
        """)
    long countUsersRankedAheadByActivityCount(@Param("activityCount") long activityCount, @Param("userId") Long userId);

    @Query("""
        select count(s)
        from UserGamificationStats s
        join User u on u.id = s.userId
        where u.deleted = false and s.totalPoints > 0
        """)
    long countUsersWithPositivePoints();

    @Query("""
        select count(s)
        from UserGamificationStats s
        join User u on u.id = s.userId
        where u.deleted = false and s.totalActivityCount > 0
        """)
    long countUsersWithPositiveActivityCount();
}
