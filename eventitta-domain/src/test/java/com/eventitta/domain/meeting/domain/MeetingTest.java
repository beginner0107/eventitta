package com.eventitta.domain.meeting.domain;

import com.eventitta.domain.meeting.dto.request.MeetingUpdateRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingTest {

    @Test
    @DisplayName("리더 ID로 리더 여부를 판단한다")
    void isLeader_checksLeaderId() {
        Meeting meeting = Meeting.builder()
            .leaderId(10L)
            .status(MeetingStatus.RECRUITING)
            .build();

        assertThat(meeting.isLeader(10L)).isTrue();
        assertThat(meeting.isLeader(11L)).isFalse();
    }

    @Test
    @DisplayName("리더 변경 시 leaderId가 갱신된다")
    void changeLeader_updatesLeaderId() {
        Meeting meeting = Meeting.builder()
            .leaderId(10L)
            .status(MeetingStatus.RECRUITING)
            .build();

        meeting.changeLeader(20L);

        assertThat(meeting.getLeaderId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("업데이트 요청으로 수정 가능한 필드가 반영된다")
    void update_appliesMutableFields() {
        Meeting meeting = Meeting.builder()
            .leaderId(10L)
            .title("old")
            .description("old desc")
            .startTime(LocalDateTime.of(2026, 3, 27, 10, 0))
            .endTime(LocalDateTime.of(2026, 3, 27, 12, 0))
            .maxMembers(4)
            .address("old address")
            .latitude(37.0)
            .longitude(127.0)
            .status(MeetingStatus.RECRUITING)
            .build();

        meeting.update(new MeetingUpdateRequest(
            "new",
            "new desc",
            LocalDateTime.of(2026, 3, 28, 10, 0),
            LocalDateTime.of(2026, 3, 28, 12, 0),
            6,
            "new address",
            38.0,
            128.0,
            MeetingStatus.CLOSED
        ));

        assertThat(meeting.getTitle()).isEqualTo("new");
        assertThat(meeting.getDescription()).isEqualTo("new desc");
        assertThat(meeting.getMaxMembers()).isEqualTo(6);
        assertThat(meeting.getAddress()).isEqualTo("new address");
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CLOSED);
    }
}
