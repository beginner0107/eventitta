package com.eventitta.domain.auth.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRefreshToken is a Querydsl query type for RefreshToken
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRefreshToken extends EntityPathBase<RefreshToken> {

    private static final long serialVersionUID = -1414965789L;

    public static final QRefreshToken refreshToken = new QRefreshToken("refreshToken");

    public final com.eventitta.domain.common.domain.QBaseTimeEntity _super = new com.eventitta.domain.common.domain.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath issuedIpMasked = createString("issuedIpMasked");

    public final StringPath issuedUserAgent = createString("issuedUserAgent");

    public final DateTimePath<java.time.LocalDateTime> lastSeenAt = createDateTime("lastSeenAt", java.time.LocalDateTime.class);

    public final StringPath lastSeenIpMasked = createString("lastSeenIpMasked");

    public final StringPath lastSeenUserAgent = createString("lastSeenUserAgent");

    public final StringPath sessionId = createString("sessionId");

    public final StringPath tokenHash = createString("tokenHash");

    public final StringPath tokenKey = createString("tokenKey");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QRefreshToken(String variable) {
        super(RefreshToken.class, forVariable(variable));
    }

    public QRefreshToken(Path<? extends RefreshToken> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRefreshToken(PathMetadata metadata) {
        super(RefreshToken.class, metadata);
    }

}

