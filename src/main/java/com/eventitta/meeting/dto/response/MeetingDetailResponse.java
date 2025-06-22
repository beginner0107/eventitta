package com.eventitta.meeting.dto.response;

import com.eventitta.meeting.domain.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "모임 상세 정보 응답")
public record MeetingDetailResponse(
    @Schema(description = "모임 ID", example = "1")
    Long id,

    @Schema(description = "모임 제목", example = "함께하는 스프링 스터디")
    String title,

    @Schema(description = "모임 설명", example = "매주 주말에 만나 스프링 심화 내용을 공부합니다.")
    String description,

    @Schema(description = "모임 시작 시간", example = "2025-08-01T10:00:00")
    LocalDateTime startTime,

    @Schema(description = "모임 종료 시간", example = "2025-08-01T12:00:00")
    LocalDateTime endTime,

    @Schema(description = "최대 참여 인원", example = "10")
    int maxMembers,

    @Schema(description = "현재 참여 인원", example = "5")
    int currentMembers,

    @Schema(description = "모임 장소 주소", example = "서울특별시 강남구 테헤란로 212")
    String address,

    @Schema(description = "위도", example = "37.5017")
    Double latitude,

    @Schema(description = "경도", example = "127.0396")
    Double longitude,

    @Schema(description = "모임 상태", example = "RECRUITING")
    MeetingStatus status,

    @Schema(description = "모임장 ID", example = "1")
    Long leaderId,

    @Schema(description = "모임장 닉네임", example = "스프링마스터")
    String leaderNickname,

    @Schema(description = "모임장 프로필 이미지 URL", example = "https://example.com/profile.jpg")
    String leaderProfileUrl,

    @Schema(description = "참여자 목록")
    List<ParticipantResponse> participants
) {
}
