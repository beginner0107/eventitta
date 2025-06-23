package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserBadge;
import com.eventitta.gamification.repository.BadgeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.gamification.repository.UserBadgeRepository;
import com.eventitta.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserActivityRepository userActivityRepository;

    @Transactional
    public void checkAndAwardBadges(User user, ActivityType activityType) {
        // 1: 첫 게시글 작성 배지
        if (activityType == ActivityType.CREATE_POST) {
            long postCount = userActivityRepository.countByUserIdAndActivityType(user.getId(), ActivityType.CREATE_POST);
            if (postCount == 1) {
                awardBadge(user, "첫 게시글");
            }
        }

        // 2: 댓글 10개 작성 배지
        if (activityType == ActivityType.CREATE_COMMENT) {
            long commentCount = userActivityRepository.countByUserIdAndActivityType(user.getId(), ActivityType.CREATE_COMMENT);
            if (commentCount >= 10) {
                awardBadge(user, "열혈 댓글러");
            }
        }

        // 3: 좋아요 50회 달성 배지
        if (activityType == ActivityType.LIKE_POST) {
            long likeCount = userActivityRepository.countByUserIdAndActivityType(user.getId(), ActivityType.LIKE_POST);
            if (likeCount >= 50) {
                awardBadge(user, "인기의 게시글");
            }
        }

        // 4: 첫 모임 참가 배지
        if (activityType == ActivityType.JOIN_MEETING) {
            long meetingCount = userActivityRepository.countByUserIdAndActivityType(user.getId(), ActivityType.JOIN_MEETING);
            if (meetingCount == 1) {
                awardBadge(user, "첫 모임 참가");
            }
        }
    }

    private void awardBadge(User user, String badgeName) {
        badgeRepository.findByName(badgeName).ifPresent(badge -> {
            boolean alreadyHasBadge = userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId());
            if (!alreadyHasBadge) {
                userBadgeRepository.save(new UserBadge(user, badge));
            }
        });
    }
}
