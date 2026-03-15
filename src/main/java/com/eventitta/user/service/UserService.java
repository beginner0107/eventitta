package com.eventitta.user.service;

import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.exception.UserErrorCode;
import com.eventitta.user.mapper.UserMapper;
import com.eventitta.user.repository.UserRepository;
import com.eventitta.user.service.dto.ChangePasswordCommand;
import com.eventitta.user.service.dto.UpdateProfileCommand;
import com.eventitta.user.service.dto.UserProfileResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserProfileResult getProfile(Long userId) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        return userMapper.toUserProfileResult(user);
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileCommand command) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        if (!user.getNickname().equals(command.nickname()) &&
            userRepository.existsByNickname(command.nickname())) {
            throw UserErrorCode.CONFLICTED_NICKNAME.defaultException();
        }
        user.updateProfile(
            command.nickname(),
            command.profilePictureUrl(),
            command.selfIntro(),
            command.interests(),
            command.address(),
            command.latitude(),
            command.longitude()
        );
        userRepository.flush();
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        user.delete();
        refreshTokenRepository.deleteByUserId(userId);
        // TODO: 탈퇴 정책 후처리 반영 필요
        // - 사용자가 리더인 모임의 리더 위임 또는 종료 처리
        // - 랭킹/캐시에서 사용자 제거
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordCommand command) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        if (!passwordEncoder.matches(command.currentPassword(), user.getPassword())) {
            throw UserErrorCode.INVALID_CURRENT_PASSWORD.defaultException();
        }
        user.changePassword(passwordEncoder.encode(command.newPassword()));
    }
}
