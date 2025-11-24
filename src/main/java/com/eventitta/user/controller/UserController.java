package com.eventitta.user.controller;

import com.eventitta.auth.annotation.CurrentUser;
import com.eventitta.common.response.ApiErrorResponse;
import com.eventitta.gamification.dto.response.ActivitySummaryResponse;
import com.eventitta.gamification.service.UserActivityService;
import com.eventitta.user.dto.ChangePasswordRequest;
import com.eventitta.user.dto.UpdateProfileRequest;
import com.eventitta.user.dto.UserProfileResponse;
import com.eventitta.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final UserActivityService userActivityService;

    @Operation(summary = "내 프로필 조회", description = "인증된 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "프로필 조회 성공", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@CurrentUser Long userId) {
        UserProfileResponse resp = userService.getProfile(userId);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "내 프로필 수정", description = "인증된 사용자의 프로필 정보를 수정합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "프로필 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (유효성 검사 실패)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/me")
    public ResponseEntity<Void> updateMyProfile(
        @CurrentUser Long userId,
        @RequestBody @Valid UpdateProfileRequest request
    ) {
        userService.updateProfile(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴", description = "인증된 사용자의 계정을 탈퇴 처리합니다. 데이터는 삭제되지 않고 'deleted' 상태로 변경됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "회원 탈퇴 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@CurrentUser Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 변경", description = "인증된 사용자의 비밀번호를 변경합니다. 현재 비밀번호를 확인 후 새 비밀번호로 교체합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
        @ApiResponse(responseCode = "400", description = "현재 비밀번호가 일치하지 않거나 새 비밀번호의 형식이 올바르지 않음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
        @CurrentUser Long userId,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/activities")
    public ResponseEntity<List<ActivitySummaryResponse>> getMyActivitySummary(@CurrentUser Long userId) {
        List<ActivitySummaryResponse> result = userActivityService.getActivitySummaryProjection(userId).stream()
            .map(summary -> new ActivitySummaryResponse(
                summary.getActivityType().toString(),
                summary.getCount(),
                summary.getTotalPoints()
            ))
            .toList();
        return ResponseEntity.ok(result);
    }
}
