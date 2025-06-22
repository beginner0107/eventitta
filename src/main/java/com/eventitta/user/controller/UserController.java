package com.eventitta.user.controller;

import com.eventitta.auth.annotation.CurrentUser;
import com.eventitta.user.dto.UserProfileResponse;
import com.eventitta.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
