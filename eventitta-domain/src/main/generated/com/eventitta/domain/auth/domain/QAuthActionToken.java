package com.eventitta.domain.auth.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAuthActionToken is a Querydsl query type for AuthActionToken
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAuthActionToken extends EntityPathBase<AuthActionToken> {

    private static final long serialVersionUID = -1687886218L;

    public static final QAuthActionToken authActionToken = new QAuthActionToken("authActionToken");

    public final com.eventitta.domain.common.domain.QBaseEntity _super = new com.eventitta.domain.common.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<AuthActionTokenPurpose> purpose = createEnum("purpose", AuthActionTokenPurpose.class);

    public final StringPath requestedIpMasked = createString("requestedIpMasked");

    public final StringPath requestedUserAgent = createString("requestedUserAgent");

    public final StringPath targetEmail = createString("targetEmail");

    public final StringPath tokenHash = createString("tokenHash");

    public final StringPath tokenKey = createString("tokenKey");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final DateTimePath<java.time.LocalDateTime> usedAt = createDateTime("usedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QAuthActionToken(String variable) {
        super(AuthActionToken.class, forVariable(variable));
    }

    public QAuthActionToken(Path<? extends AuthActionToken> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAuthActionToken(PathMetadata metadata) {
        super(AuthActionToken.class, metadata);
    }

}

