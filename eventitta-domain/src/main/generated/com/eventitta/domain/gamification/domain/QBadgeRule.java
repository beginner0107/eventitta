package com.eventitta.domain.gamification.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBadgeRule is a Querydsl query type for BadgeRule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBadgeRule extends EntityPathBase<BadgeRule> {

    private static final long serialVersionUID = -1403335305L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBadgeRule badgeRule = new QBadgeRule("badgeRule");

    public final com.eventitta.domain.common.domain.QBaseTimeEntity _super = new com.eventitta.domain.common.domain.QBaseTimeEntity(this);

    public final EnumPath<ActivityType> activityType = createEnum("activityType", ActivityType.class);

    public final QBadge badge;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final BooleanPath enabled = createBoolean("enabled");

    public final EnumPath<EvaluationType> evaluationType = createEnum("evaluationType", EvaluationType.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> threshold = createNumber("threshold", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QBadgeRule(String variable) {
        this(BadgeRule.class, forVariable(variable), INITS);
    }

    public QBadgeRule(Path<? extends BadgeRule> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBadgeRule(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBadgeRule(PathMetadata metadata, PathInits inits) {
        this(BadgeRule.class, metadata, inits);
    }

    public QBadgeRule(Class<? extends BadgeRule> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.badge = inits.isInitialized("badge") ? new QBadge(forProperty("badge")) : null;
    }

}

