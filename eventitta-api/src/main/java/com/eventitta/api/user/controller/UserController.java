package com.eventitta.api.user.controller;

import com.eventitta.api.auth.security.annotation.CurrentUser;
import com.eventitta.api.common.response.ApiErrorResponse;
import com.eventitta.api.auth.security.UserPrincipal;
import com.eventitta.api.auth.controller.request.SocialLoginRequest;
import com.eventitta.api.auth.AuthConstants;
import com.eventitta.api.auth.controller.AuthMapper;
import com.eventitta.api.auth.cookie.CookieManager;
import com.eventitta.api.auth.oauth.kakao.KakaoAuthorizationSupport;
import com.eventitta.domain.gamification.api.internal.facade.GamificationQueryFacade;
import com.eventitta.domain.gamification.api.internal.view.ActivitySummaryView;
import com.eventitta.api.user.controller.request.ChangePasswordRequest;
import com.eventitta.api.user.controller.request.SetLocalPasswordRequest;
import com.eventitta.api.user.controller.request.UpdateProfileRequest;
import com.eventitta.api.user.controller.response.UserSessionResponse;
import com.eventitta.api.user.controller.response.UserProfileResponse;
import com.eventitta.api.user.mapper.UserMapper;
import com.eventitta.domain.auth.service.AuthService;
import com.eventitta.domain.auth.service.dto.AuthSessionMetadata;
import com.eventitta.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final AuthService authService;
    private final GamificationQueryFacade gamificationQueryService;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;
    private final KakaoAuthorizationSupport kakaoAuthorizationSupport;
    private final CookieManager cookieManager;

    @Operation(summary = "내 프로필 조회", description = "인증된 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "프로필 조회 성공", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@CurrentUser Long userId) {
        var result = userService.getProfile(userId);
        return ResponseEntity.ok(userMapper.toUserProfileResponse(result));
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
        userService.updateProfile(userId, userMapper.toUpdateProfileCommand(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴", description = "인증된 사용자의 계정을 탈퇴 처리합니다. 데이터는 삭제되지 않고 'deleted' 상태로 변경됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "회원 탈퇴 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@CurrentUser Long userId, HttpServletResponse response) {
        userService.deleteUser(userId);
        cookieManager.deleteTokenCookies(response);
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
        @Valid @RequestBody ChangePasswordRequest request,
        HttpServletResponse response
    ) {
        userService.changePassword(userId, userMapper.toChangePasswordCommand(request));
        cookieManager.deleteTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "소셜 계정에 로컬 비밀번호 설정", description = "현재 로그인된 소셜 계정에 로컬 로그인용 비밀번호를 설정합니다.")
    @PostMapping("/me/password/local")
    public ResponseEntity<Void> setLocalPassword(
        @CurrentUser Long userId,
        @Valid @RequestBody SetLocalPasswordRequest request,
        HttpServletResponse response
    ) {
        authService.setLocalPassword(userId, request.newPassword());
        cookieManager.deleteTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 계정에 카카오 로그인 연결", description = "현재 로그인된 계정에 카카오 로그인을 연결합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "카카오 로그인 연결 성공"),
        @ApiResponse(responseCode = "401", description = "state 검증 실패 또는 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 연결된 소셜 계정 또는 연결 필요", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/me/social/kakao/link")
    public ResponseEntity<Void> linkKakao(
        @CurrentUser Long userId,
        @Valid @RequestBody SocialLoginRequest request,
        @CookieValue(name = AuthConstants.OAUTH_STATE, required = false) String oauthState,
        jakarta.servlet.http.HttpServletResponse response
    ) {
        try {
            kakaoAuthorizationSupport.validateState(oauthState, request.state());
            authService.linkKakao(userId, authMapper.toKakaoLinkCommand(request));
            return ResponseEntity.noContent().build();
        } finally {
            cookieManager.deleteCookie(response, AuthConstants.OAUTH_STATE);
        }
    }

    @Operation(summary = "내 계정의 카카오 로그인 연결 해제", description = "현재 로그인된 계정에서 카카오 로그인을 해제합니다.")
    @DeleteMapping("/me/social/kakao/link")
    public ResponseEntity<Void> unlinkKakao(@CurrentUser Long userId, HttpServletResponse response) {
        authService.unlinkKakao(userId);
        cookieManager.deleteTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 세션 목록 조회", description = "현재 계정의 활성 세션 목록을 조회합니다.")
    @GetMapping("/me/sessions")
    public ResponseEntity<List<UserSessionResponse>> getMySessions(@AuthenticationPrincipal UserPrincipal principal) {
        List<UserSessionResponse> response = authService.getSessions(principal.getId(), principal.getSessionId()).stream()
            .map(this::toUserSessionResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "특정 세션 종료", description = "선택한 세션 하나를 종료합니다.")
    @DeleteMapping("/me/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
        @CurrentUser Long userId,
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable String sessionId,
        HttpServletResponse response
    ) {
        boolean revokedCurrent = authService.revokeSession(userId, sessionId, principal.getSessionId());
        if (revokedCurrent) {
            cookieManager.deleteTokenCookies(response);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "모든 세션 종료", description = "현재 계정의 모든 세션을 종료합니다.")
    @DeleteMapping("/me/sessions")
    public ResponseEntity<Void> revokeAllSessions(
        @CurrentUser Long userId,
        @AuthenticationPrincipal UserPrincipal principal,
        HttpServletResponse response
    ) {
        authService.revokeAllSessions(userId, principal.getSessionId());
        cookieManager.deleteTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/activities")
    public ResponseEntity<List<ActivitySummaryView>> getMyActivitySummary(@CurrentUser Long userId) {
        return ResponseEntity.ok(gamificationQueryService.getActivitySummary(userId));
    }

    private UserSessionResponse toUserSessionResponse(AuthSessionMetadata session) {
        return new UserSessionResponse(
            session.sessionId(),
            session.issuedAt(),
            session.lastSeenAt(),
            session.expiresAt(),
            session.issuedIpMasked(),
            session.issuedUserAgent(),
            session.lastSeenIpMasked(),
            session.lastSeenUserAgent(),
            session.current()
        );
    }
}
