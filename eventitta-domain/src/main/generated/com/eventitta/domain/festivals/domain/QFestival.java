package com.eventitta.domain.festivals.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QFestival is a Querydsl query type for Festival
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFestival extends EntityPathBase<Festival> {

    private static final long serialVersionUID = -989784210L;

    public static final QFestival festival = new QFestival("festival");

    public final com.eventitta.domain.common.domain.QBaseTimeEntity _super = new com.eventitta.domain.common.domain.QBaseTimeEntity(this);

    public final StringPath category = createString("category");

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<DataSource> dataSource = createEnum("dataSource", DataSource.class);

    public final StringPath detailUrl = createString("detailUrl");

    public final StringPath district = createString("district");

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final StringPath externalId = createString("externalId");

    public final StringPath feeInfo = createString("feeInfo");

    public final StringPath homepageUrl = createString("homepageUrl");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isFree = createBoolean("isFree");

    public final NumberPath<java.math.BigDecimal> latitude = createNumber("latitude", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> longitude = createNumber("longitude", java.math.BigDecimal.class);

    public final StringPath mainImageUrl = createString("mainImageUrl");

    public final StringPath organizer = createString("organizer");

    public final StringPath performers = createString("performers");

    public final StringPath programInfo = createString("programInfo");

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final StringPath targetAudience = createString("targetAudience");

    public final StringPath themeCode = createString("themeCode");

    public final StringPath ticketType = createString("ticketType");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath venue = createString("venue");

    public QFestival(String variable) {
        super(Festival.class, forVariable(variable));
    }

    public QFestival(Path<? extends Festival> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFestival(PathMetadata metadata) {
        super(Festival.class, metadata);
    }

}

