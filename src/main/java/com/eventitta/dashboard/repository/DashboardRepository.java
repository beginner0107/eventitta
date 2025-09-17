package com.eventitta.dashboard.repository;

import com.eventitta.dashboard.dto.response.UserRankingResponse;
import com.eventitta.gamification.domain.QUserActivity;
import com.eventitta.user.domain.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {

    private final JPAQueryFactory queryFactory;

    public List<UserRankingResponse> findTopRankings(LocalDateTime from, int limit) {
        QUserActivity ua = QUserActivity.userActivity;
        QUser user = QUser.user;

        var query = queryFactory
            .select(Projections.constructor(
                UserRankingResponse.class,
                user.nickname,
                ua.pointsEarned.sum().intValue(),
                ua.createdAt.max()
            ))
            .from(ua)
            .join(ua.user, user);

        if (from != null) {
            query.where(ua.createdAt.goe(from));
        }

        return query
            .groupBy(user.id, user.nickname)
            .orderBy(ua.pointsEarned.sum().desc())
            .limit(limit)
            .fetch();
    }
}
