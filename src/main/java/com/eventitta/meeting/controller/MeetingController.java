package com.eventitta.meeting.controller;

import com.eventitta.auth.annotation.CurrentUser;
import com.eventitta.common.response.ApiErrorResponse;
import com.eventitta.meeting.dto.MeetingCreateRequest;
import com.eventitta.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

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
}
