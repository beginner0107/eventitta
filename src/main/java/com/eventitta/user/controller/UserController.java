package com.eventitta.user.controller;

import com.eventitta.auth.annotation.CurrentUser;
import com.eventitta.user.dto.ChangePasswordRequest;
import com.eventitta.user.dto.UpdateProfileRequest;
import com.eventitta.user.dto.UserProfileResponse;
import com.eventitta.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@CurrentUser Long userId) {
        UserProfileResponse resp = userService.getProfile(userId);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "내 프로필 수정")
    @PutMapping("/me")
    public ResponseEntity<Void> updateMyProfile(
        @CurrentUser Long userId,
        @RequestBody @Valid UpdateProfileRequest request
    ) {
        userService.updateProfile(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@CurrentUser Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 변경")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
    })
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
        @CurrentUser Long userId,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
