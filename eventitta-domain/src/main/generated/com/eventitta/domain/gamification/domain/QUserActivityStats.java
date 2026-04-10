package com.eventitta.domain.gamification.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserActivityStats is a Querydsl query type for UserActivityStats
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserActivityStats extends EntityPathBase<UserActivityStats> {

    private static final long serialVersionUID = 409432317L;

    public static final QUserActivityStats userActivityStats = new QUserActivityStats("userActivityStats");

    public final NumberPath<Long> actionCount = createNumber("actionCount", Long.class);

    public final EnumPath<RewardActionType> actionType = createEnum("actionType", RewardActionType.class);

    public final NumberPath<Integer> pointsTotal = createNumber("pointsTotal", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUserActivityStats(String variable) {
        super(UserActivityStats.class, forVariable(variable));
    }

    public QUserActivityStats(Path<? extends UserActivityStats> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserActivityStats(PathMetadata metadata) {
        super(UserActivityStats.class, metadata);
    }

}

