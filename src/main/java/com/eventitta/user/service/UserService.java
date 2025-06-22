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
        User user = userRepository.findById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        user.updateProfile(
            req.nickname(),
            req.profilePictureUrl(),
            req.selfIntro(),
            req.interests(),
            req.address(),
            req.latitude(),
            req.longitude()
        );
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        userRepository.delete(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw UserErrorCode.INVALID_CURRENT_PASSWORD.defaultException();
        }
        user.changePassword(passwordEncoder.encode(req.newPassword()));
    }
}
