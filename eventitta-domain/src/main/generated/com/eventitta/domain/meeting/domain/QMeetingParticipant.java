package com.eventitta.domain.meeting.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMeetingParticipant is a Querydsl query type for MeetingParticipant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMeetingParticipant extends EntityPathBase<MeetingParticipant> {

    private static final long serialVersionUID = 1324895342L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMeetingParticipant meetingParticipant = new QMeetingParticipant("meetingParticipant");

    public final com.eventitta.domain.common.domain.QBaseTimeEntity _super = new com.eventitta.domain.common.domain.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMeeting meeting;

    public final EnumPath<ParticipantStatus> status = createEnum("status", ParticipantStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QMeetingParticipant(String variable) {
        this(MeetingParticipant.class, forVariable(variable), INITS);
    }

    public QMeetingParticipant(Path<? extends MeetingParticipant> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMeetingParticipant(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMeetingParticipant(PathMetadata metadata, PathInits inits) {
        this(MeetingParticipant.class, metadata, inits);
    }

    public QMeetingParticipant(Class<? extends MeetingParticipant> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.meeting = inits.isInitialized("meeting") ? new QMeeting(forProperty("meeting")) : null;
    }

}

