package com.eventitta.domain.gamification.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserGamificationStats is a Querydsl query type for UserGamificationStats
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserGamificationStats extends EntityPathBase<UserGamificationStats> {

    private static final long serialVersionUID = 1034168865L;

    public static final QUserGamificationStats userGamificationStats = new QUserGamificationStats("userGamificationStats");

    public final NumberPath<Long> totalActivityCount = createNumber("totalActivityCount", Long.class);

    public final NumberPath<Integer> totalPoints = createNumber("totalPoints", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUserGamificationStats(String variable) {
        super(UserGamificationStats.class, forVariable(variable));
    }

    public QUserGamificationStats(Path<? extends UserGamificationStats> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserGamificationStats(PathMetadata metadata) {
        super(UserGamificationStats.class, metadata);
    }

}

