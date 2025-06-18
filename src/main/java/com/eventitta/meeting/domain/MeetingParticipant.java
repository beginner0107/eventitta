package com.eventitta.meeting.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @Builder
    public MeetingParticipant(Meeting meeting, Long userId, ParticipantStatus status) {
        this.meeting = meeting;
        this.userId = userId;
        this.status = status;
    }

    public void approve() {
        this.status = ParticipantStatus.APPROVED;
    }

    public void reject() {
        this.status = ParticipantStatus.REJECTED;
    }
}
