package com.eventitta.user.service;

import com.eventitta.user.domain.User;
import com.eventitta.user.dto.ChangePasswordRequest;
import com.eventitta.user.dto.UpdateProfileRequest;
import com.eventitta.user.dto.UserProfileResponse;
import com.eventitta.user.exception.UserErrorCode;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        if (!user.getNickname().equals(req.nickname()) &&
            userRepository.existsByNickname(req.nickname())) {
            throw UserErrorCode.CONFLICTED_NICKNAME.defaultException();
        }
        user.updateProfile(
            req.nickname(),
            req.profilePictureUrl(),
            req.selfIntro(),
            req.interests(),
            req.address(),
            req.latitude(),
            req.longitude()
        );
        userRepository.flush();
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        user.delete();
        // TODO: 탈퇴 정책 후처리 반영 필요
        // - 리프레시 토큰 전체 삭제
        // - 사용자가 리더인 모임의 리더 위임 또는 종료 처리
        // - 랭킹/캐시에서 사용자 제거
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw UserErrorCode.INVALID_CURRENT_PASSWORD.defaultException();
        }
        user.changePassword(passwordEncoder.encode(req.newPassword()));
    }
}
