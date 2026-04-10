package com.eventitta.domain.media.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMediaAssetVariant is a Querydsl query type for MediaAssetVariant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMediaAssetVariant extends EntityPathBase<MediaAssetVariant> {

    private static final long serialVersionUID = 1316293370L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMediaAssetVariant mediaAssetVariant = new QMediaAssetVariant("mediaAssetVariant");

    public final com.eventitta.domain.common.domain.QBaseEntity _super = new com.eventitta.domain.common.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final StringPath format = createString("format");

    public final NumberPath<Integer> height = createNumber("height", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMediaAsset mediaAsset;

    public final StringPath publicUrl = createString("publicUrl");

    public final NumberPath<Long> sizeBytes = createNumber("sizeBytes", Long.class);

    public final EnumPath<MediaProcessingStatus> status = createEnum("status", MediaProcessingStatus.class);

    public final StringPath storageKey = createString("storageKey");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final EnumPath<MediaVariantType> variantType = createEnum("variantType", MediaVariantType.class);

    public final NumberPath<Integer> width = createNumber("width", Integer.class);

    public QMediaAssetVariant(String variable) {
        this(MediaAssetVariant.class, forVariable(variable), INITS);
    }

    public QMediaAssetVariant(Path<? extends MediaAssetVariant> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMediaAssetVariant(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMediaAssetVariant(PathMetadata metadata, PathInits inits) {
        this(MediaAssetVariant.class, metadata, inits);
    }

    public QMediaAssetVariant(Class<? extends MediaAssetVariant> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.mediaAsset = inits.isInitialized("mediaAsset") ? new QMediaAsset(forProperty("mediaAsset")) : null;
    }

}

