package com.eventitta.user.service;

import com.eventitta.user.domain.User;
import com.eventitta.user.dto.UserProfileResponse;
import com.eventitta.user.exception.UserErrorCode;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);
        return UserProfileResponse.from(user);
    }
}
