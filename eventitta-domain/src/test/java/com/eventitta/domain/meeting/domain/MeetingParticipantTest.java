package com.eventitta.domain.meeting.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingParticipantTest {

    @Test
    @DisplayName("참가자 생성 시 userId와 상태가 저장된다")
    void builder_storesScalarUserId() {
        Meeting meeting = Meeting.builder()
            .leaderId(1L)
            .status(MeetingStatus.RECRUITING)
            .build();

        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(meeting)
            .userId(2L)
            .status(ParticipantStatus.PENDING)
            .build();

        assertThat(participant.getMeeting()).isEqualTo(meeting);
        assertThat(participant.getUserId()).isEqualTo(2L);
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.PENDING);
    }

    @Test
    @DisplayName("approve/reject는 상태를 직접 갱신한다")
    void approveAndReject_updateStatus() {
        MeetingParticipant participant = MeetingParticipant.builder()
            .meeting(Meeting.builder().leaderId(1L).status(MeetingStatus.RECRUITING).build())
            .userId(2L)
            .status(ParticipantStatus.PENDING)
            .build();

        participant.approve();
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.APPROVED);

        participant.reject();
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.REJECTED);
    }
}
