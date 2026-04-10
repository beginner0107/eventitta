package com.eventitta.domain.meeting.domain;

import com.eventitta.domain.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "meeting_participants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @Builder
    public MeetingParticipant(Meeting meeting, Long userId, ParticipantStatus status) {
        this.meeting = meeting;
        this.userId = userId;
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void approve() {
        this.status = ParticipantStatus.APPROVED;
    }

    public void reject() {
        this.status = ParticipantStatus.REJECTED;
    }
}
