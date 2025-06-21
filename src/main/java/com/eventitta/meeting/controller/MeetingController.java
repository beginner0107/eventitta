package com.eventitta.meeting.controller;

import com.eventitta.auth.annotation.CurrentUser;
import com.eventitta.common.response.ApiErrorResponse;
import com.eventitta.common.response.PageResponse;
import com.eventitta.meeting.dto.*;
import com.eventitta.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "Meeting", description = "모임 관련 API")
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(summary = "모임 생성", description = "새로운 모임을 생성합니다. 인증된 사용자의 ID는 토큰에서 추출하여 사용합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "모임 생성 성공",
            content = @Content(schema = @Schema(hidden = true))),
    })
    @PostMapping
    public ResponseEntity<Void> createMeeting(@CurrentUser Long userId,
                                              @RequestBody @Valid MeetingCreateRequest request) {
        Long meetingId = meetingService.createMeeting(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/meetings/" + meetingId)).build();
    }

    @Operation(summary = "모임 수정", description = "모임 정보를 수정합니다. 모임 리더만 수정할 수 있습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "모임 수정 성공",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "모임 리더가 아닌 경우",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "모임을 찾을 수 없는 경우",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{meetingId}")
    public ResponseEntity<Void> updateMeeting(@CurrentUser Long userId,
                                              @PathVariable Long meetingId,
                                              @RequestBody @Valid MeetingUpdateRequest request) {
        meetingService.updateMeeting(userId, meetingId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "모임 삭제", description = "모임을 삭제합니다. 모임 리더만 삭제할 수 있습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "모임 삭제 성공"),
        @ApiResponse(responseCode = "403", description = "모임 리더가 아닌 경우",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "모임을 찾을 수 없는 경우",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "이미 삭제된 모임인 경우",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Void> deleteMeeting(@CurrentUser Long userId,
                                              @PathVariable Long meetingId) {
        meetingService.deleteMeeting(userId, meetingId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "모임 상세 조회", description = "특정 모임의 상세 정보와 참여자 목록을 조회합니다. 인증된 사용자가 아니더라도 조회 가능합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "모임 조회 성공",
            content = @Content(schema = @Schema(implementation = MeetingDetailResponse.class))),
        @ApiResponse(responseCode = "404", description = "모임을 찾을 수 없는 경우",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "이미 삭제된 모임인 경우",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{meetingId}")
    public ResponseEntity<MeetingDetailResponse> getMeetingDetail(
        @Parameter(description = "모임 ID", required = true)
        @PathVariable Long meetingId) {

        MeetingDetailResponse response = meetingService.getMeetingDetail(meetingId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "모임 목록 조회", description = "다양한 필터 조건으로 모임 목록을 조회합니다. 인증 여부와 관계없이 조회 가능합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "모임 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<PageResponse<MeetingSummaryResponse>> getMeetings(
        @Valid MeetingFilter filter
    ) {
        PageResponse<MeetingSummaryResponse> response = meetingService.getMeetings(filter);
        return ResponseEntity.ok(response);
    }
}
