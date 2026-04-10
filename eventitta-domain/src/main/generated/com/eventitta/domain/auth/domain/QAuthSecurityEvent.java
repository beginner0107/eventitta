package com.eventitta.domain.auth.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAuthSecurityEvent is a Querydsl query type for AuthSecurityEvent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAuthSecurityEvent extends EntityPathBase<AuthSecurityEvent> {

    private static final long serialVersionUID = 466600845L;

    public static final QAuthSecurityEvent authSecurityEvent = new QAuthSecurityEvent("authSecurityEvent");

    public final com.eventitta.domain.common.domain.QBaseTimeEntity _super = new com.eventitta.domain.common.domain.QBaseTimeEntity(this);

    public final StringPath clientIpMasked = createString("clientIpMasked");

    public final StringPath clientUserAgent = createString("clientUserAgent");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath detailMessage = createString("detailMessage");

    public final EnumPath<AuthSecurityEventType> eventType = createEnum("eventType", AuthSecurityEventType.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath identifier = createString("identifier");

    public final EnumPath<AuthSecurityEventOutcome> outcome = createEnum("outcome", AuthSecurityEventOutcome.class);

    public final StringPath sessionId = createString("sessionId");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QAuthSecurityEvent(String variable) {
        super(AuthSecurityEvent.class, forVariable(variable));
    }

    public QAuthSecurityEvent(Path<? extends AuthSecurityEvent> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAuthSecurityEvent(PathMetadata metadata) {
        super(AuthSecurityEvent.class, metadata);
    }

}

