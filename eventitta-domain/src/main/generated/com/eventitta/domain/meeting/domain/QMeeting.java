package com.eventitta.domain.meeting.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMeeting is a Querydsl query type for Meeting
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMeeting extends EntityPathBase<Meeting> {

    private static final long serialVersionUID = -1154804347L;

    public static final QMeeting meeting = new QMeeting("meeting");

    public final com.eventitta.domain.common.domain.QBaseEntity _super = new com.eventitta.domain.common.domain.QBaseEntity(this);

    public final StringPath address = createString("address");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final NumberPath<Integer> currentMembers = createNumber("currentMembers", Integer.class);

    public final BooleanPath deleted = createBoolean("deleted");

    public final StringPath description = createString("description");

    public final DateTimePath<java.time.LocalDateTime> endTime = createDateTime("endTime", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final NumberPath<Long> leaderId = createNumber("leaderId", Long.class);

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final NumberPath<Integer> maxMembers = createNumber("maxMembers", Integer.class);

    public final ListPath<MeetingParticipant, QMeetingParticipant> participants = this.<MeetingParticipant, QMeetingParticipant>createList("participants", MeetingParticipant.class, QMeetingParticipant.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> startTime = createDateTime("startTime", java.time.LocalDateTime.class);

    public final EnumPath<MeetingStatus> status = createEnum("status", MeetingStatus.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public QMeeting(String variable) {
        super(Meeting.class, forVariable(variable));
    }

    public QMeeting(Path<? extends Meeting> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMeeting(PathMetadata metadata) {
        super(Meeting.class, metadata);
    }

}

