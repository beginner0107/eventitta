package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.exception.UserErrorCode;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Service
@RequiredArgsConstructor
public class UserActivityService {
    private final UserActivityRepository userActivityRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;

    @Transactional
    public void recordActivity(Long userId, ActivityType activityType, Long targetId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        // 1. 사용자 활동 내역 생성 및 저장
        boolean exists = userActivityRepository
            .findByUserIdAndActivityTypeAndTargetId(userId, activityType, targetId)
            .isPresent();
        if (exists) {
            return;
        }

        // 2. 사용자 포인트 업데이트
        user.addPoints(activityType.getPoints());

        // 3. 배지 획득 조건 검사
        badgeService.checkAndAwardBadges(user, activityType);
    }

    @Transactional
    public void revokeActivity(Long userId, ActivityType activityType, Long targetId) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserErrorCode.NOT_FOUND_USER_ID::defaultException);

        Optional<UserActivity> activityToRevoke = userActivityRepository
            .findByUserIdAndActivityTypeAndTargetId(userId, activityType, targetId);

        if (activityToRevoke.isPresent()) {
            user.subtractPoints(activityType.getPoints());
            userActivityRepository.delete(activityToRevoke.get());
        }
    }
}
