package com.eventitta.domain.media.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMediaAsset is a Querydsl query type for MediaAsset
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMediaAsset extends EntityPathBase<MediaAsset> {

    private static final long serialVersionUID = 272050955L;

    public static final QMediaAsset mediaAsset = new QMediaAsset("mediaAsset");

    public final com.eventitta.domain.common.domain.QBaseEntity _super = new com.eventitta.domain.common.domain.QBaseEntity(this);

    public final DateTimePath<java.time.LocalDateTime> attachedAt = createDateTime("attachedAt", java.time.LocalDateTime.class);

    public final EnumPath<MediaCategory> category = createEnum("category", MediaCategory.class);

    public final StringPath checksum = createString("checksum");

    public final StringPath contentType = createString("contentType");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> height = createNumber("height", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath originalFilename = createString("originalFilename");

    public final NumberPath<Long> ownerUserId = createNumber("ownerUserId", Long.class);

    public final StringPath processingError = createString("processingError");

    public final EnumPath<MediaProcessingStatus> processingStatus = createEnum("processingStatus", MediaProcessingStatus.class);

    public final StringPath publicUrl = createString("publicUrl");

    public final NumberPath<Long> sizeBytes = createNumber("sizeBytes", Long.class);

    public final EnumPath<MediaAssetStatus> status = createEnum("status", MediaAssetStatus.class);

    public final StringPath storageKey = createString("storageKey");

    public final EnumPath<MediaStorageProvider> storageProvider = createEnum("storageProvider", MediaStorageProvider.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final ListPath<MediaAssetVariant, QMediaAssetVariant> variants = this.<MediaAssetVariant, QMediaAssetVariant>createList("variants", MediaAssetVariant.class, QMediaAssetVariant.class, PathInits.DIRECT2);

    public final NumberPath<Integer> width = createNumber("width", Integer.class);

    public QMediaAsset(String variable) {
        super(MediaAsset.class, forVariable(variable));
    }

    public QMediaAsset(Path<? extends MediaAsset> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMediaAsset(PathMetadata metadata) {
        super(MediaAsset.class, metadata);
    }

}

