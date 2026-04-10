package com.eventitta.domain.gamification.service;

import com.eventitta.domain.gamification.domain.RewardActionType;
import com.eventitta.domain.gamification.domain.UserActivityStats;
import com.eventitta.domain.gamification.domain.UserGamificationStats;
import com.eventitta.domain.gamification.api.internal.facade.GamificationInternalFacade;
import com.eventitta.domain.gamification.event.GamificationStateChangedEvent;
import com.eventitta.domain.gamification.repository.GamificationActionRecordRepository;
import com.eventitta.domain.gamification.repository.UserActivityStatsRepository;
import com.eventitta.domain.gamification.repository.UserBadgeRepository;
import com.eventitta.domain.gamification.repository.UserGamificationStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultGamificationFacade implements GamificationInternalFacade {

    private final GamificationActionRecordRepository actionRecordRepository;
    private final UserGamificationStatsRepository userGamificationStatsRepository;
    private final UserActivityStatsRepository userActivityStatsRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void onPostCreated(Long userId, Long postId) {
        grant(userId, RewardActionType.CREATE_POST, postId);
    }

    @Override
    @Transactional
    public void onPostDeleted(Long userId, Long postId) {
        revoke(userId, RewardActionType.CREATE_POST, postId);
    }

    @Override
    @Transactional
    public void onCommentCreated(Long userId, Long commentId) {
        grant(userId, RewardActionType.CREATE_COMMENT, commentId);
    }

    @Override
    @Transactional
    public void onCommentDeleted(Long userId, Long commentId) {
        revoke(userId, RewardActionType.CREATE_COMMENT, commentId);
    }

    @Override
    @Transactional
    public void onMeetingJoinApproved(Long userId, Long meetingId) {
        grant(userId, RewardActionType.JOIN_MEETING, meetingId);
    }

    @Override
    @Transactional
    public void onMeetingJoinCancelled(Long userId, Long meetingId) {
        revoke(userId, RewardActionType.JOIN_MEETING, meetingId);
    }

    @Override
    @Transactional
    public void removeUserData(Long userId) {
        userBadgeRepository.deleteByUserId(userId);
        actionRecordRepository.deleteByUserId(userId);
        userActivityStatsRepository.deleteByUserId(userId);
        userGamificationStatsRepository.deleteById(userId);
    }

    private void grant(Long userId, RewardActionType actionType, Long targetId) {
        boolean inserted = actionRecordRepository.insertGrantIfAbsent(
            userId,
            actionType.getLegacyType(),
            actionType.getResourceType(),
            targetId,
            actionType.getDefaultPoint()
        );

        if (!inserted) {
            log.debug("[Gamification] Skipped duplicate grant. userId={}, actionType={}, targetId={}",
                userId, actionType, targetId);
            return;
        }

        userGamificationStatsRepository.increment(userId, actionType.getDefaultPoint(), 1);
        userActivityStatsRepository.increment(userId, actionType, actionType.getDefaultPoint());
        publishStateChanged(userId, actionType);
    }

    private void revoke(Long userId, RewardActionType actionType, Long targetId) {
        long deletedCount = actionRecordRepository.deleteByUserIdAndActivityTypeAndTargetId(
            userId,
            actionType.getLegacyType(),
            targetId
        );

        if (deletedCount == 0) {
            return;
        }

        userGamificationStatsRepository.decrement(userId, actionType.getDefaultPoint(), 1);
        userActivityStatsRepository.decrement(userId, actionType, actionType.getDefaultPoint());
        publishStateChanged(userId, actionType);
    }

    private void publishStateChanged(Long userId, RewardActionType actionType) {
        UserGamificationStats totalStats = userGamificationStatsRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Missing gamification stats for userId=" + userId));
        UserActivityStats actionStats = userActivityStatsRepository.findByUserIdAndActionType(userId, actionType)
            .orElse(null);

        eventPublisher.publishEvent(new GamificationStateChangedEvent(
            userId,
            actionType,
            totalStats.getTotalPoints(),
            totalStats.getTotalActivityCount(),
            actionStats != null ? actionStats.getActionCount() : 0L,
            actionStats != null ? actionStats.getPointsTotal() : 0
        ));
    }
}
