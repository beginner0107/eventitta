package com.eventitta.domain.auth.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAuthIdentity is a Querydsl query type for AuthIdentity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAuthIdentity extends EntityPathBase<AuthIdentity> {

    private static final long serialVersionUID = -1900799861L;

    public static final QAuthIdentity authIdentity = new QAuthIdentity("authIdentity");

    public final com.eventitta.domain.common.domain.QBaseEntity _super = new com.eventitta.domain.common.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final BooleanPath emailVerified = createBoolean("emailVerified");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<AuthProvider> provider = createEnum("provider", AuthProvider.class);

    public final StringPath providerEmail = createString("providerEmail");

    public final StringPath providerUserId = createString("providerUserId");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QAuthIdentity(String variable) {
        super(AuthIdentity.class, forVariable(variable));
    }

    public QAuthIdentity(Path<? extends AuthIdentity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAuthIdentity(PathMetadata metadata) {
        super(AuthIdentity.class, metadata);
    }

}

