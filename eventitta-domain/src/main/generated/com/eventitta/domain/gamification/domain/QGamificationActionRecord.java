package com.eventitta.domain.gamification.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGamificationActionRecord is a Querydsl query type for GamificationActionRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGamificationActionRecord extends EntityPathBase<GamificationActionRecord> {

    private static final long serialVersionUID = 894508922L;

    public static final QGamificationActionRecord gamificationActionRecord = new QGamificationActionRecord("gamificationActionRecord");

    public final com.eventitta.domain.common.domain.QBaseTimeEntity _super = new com.eventitta.domain.common.domain.QBaseTimeEntity(this);

    public final EnumPath<ActivityType> activityType = createEnum("activityType", ActivityType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> pointsEarned = createNumber("pointsEarned", Integer.class);

    public final EnumPath<ResourceType> resourceType = createEnum("resourceType", ResourceType.class);

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QGamificationActionRecord(String variable) {
        super(GamificationActionRecord.class, forVariable(variable));
    }

    public QGamificationActionRecord(Path<? extends GamificationActionRecord> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGamificationActionRecord(PathMetadata metadata) {
        super(GamificationActionRecord.class, metadata);
    }

}

