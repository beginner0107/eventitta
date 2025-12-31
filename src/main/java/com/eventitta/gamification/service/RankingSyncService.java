package com.eventitta.gamification.service;

import com.eventitta.gamification.constants.RankingConstants;
import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 순위 동기화 서비스
 * MySQL 데이터를 Redis로 동기화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingSyncService {

    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final RankingService rankingService;

    /**
     * MySQL → Redis 전체 동기화
     * 배치 처리를 통한 성능 최적화
     */
    @Transactional(readOnly = true)
    public void syncAllRankingsFromDatabase() {
        long startTime = System.currentTimeMillis();
        log.info("[RankingSync] Starting full sync from MySQL to Redis");

        try {
            syncPointsRanking();
            syncActivityCountRanking();

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[RankingSync] Full sync completed in {} ms", elapsedTime);
        } catch (Exception e) {
            log.error("[RankingSync] Failed to sync rankings from database", e);
            throw new RuntimeException("Failed to sync rankings", e);
        }
    }

    /**
     * 포인트 순위 동기화
     * 페이징을 통한 메모리 효율적 처리
     */
    private void syncPointsRanking() {
        log.info("[RankingSync] Starting points ranking sync");

        long totalUsers = userRepository.count();
        int batchSize = RankingConstants.SYNC_BATCH_SIZE;
        int totalPages = (int) Math.ceil((double) totalUsers / batchSize);

        log.info("[RankingSync] Syncing points for {} users in {} batches",
            totalUsers, totalPages);

        for (int page = 0; page < totalPages; page++) {
            Pageable pageable = PageRequest.of(page, batchSize);
            Page<User> userPage = userRepository.findAll(pageable);

            Map<Long, Double> userScores = userPage.getContent().stream()
                .filter(user -> user.getPoints() > 0)  // 0점 유저는 제외
                .collect(Collectors.toMap(
                    User::getId,
                    user -> (double) user.getPoints()
                ));

            if (!userScores.isEmpty()) {
                rankingService.updateScoresBatch(RankingType.POINTS, userScores);
                log.debug("[RankingSync] Synced {} users' points (page {}/{})",
                    userScores.size(), page + 1, totalPages);
            }
        }

        log.info("[RankingSync] Points ranking sync completed");
    }

    /**
     * 활동량 순위 동기화
     * 각 유저별 활동 수를 계산하여 동기화
     */
    private void syncActivityCountRanking() {
        log.info("[RankingSync] Starting activity count ranking sync");

        long totalUsers = userRepository.count();
        int batchSize = RankingConstants.SYNC_BATCH_SIZE;
        int totalPages = (int) Math.ceil((double) totalUsers / batchSize);

        log.info("[RankingSync] Syncing activity counts for {} users in {} batches",
            totalUsers, totalPages);

        for (int page = 0; page < totalPages; page++) {
            Pageable pageable = PageRequest.of(page, batchSize);
            List<Long> userIds = userRepository.findAll(pageable)
                .map(User::getId)
                .toList();

            Map<Long, Double> userActivityCounts = new HashMap<>();

            // 각 유저별 활동 수 계산
            for (Long userId : userIds) {
                long activityCount = userActivityRepository.countByUserId(userId);
                if (activityCount > 0) {
                    userActivityCounts.put(userId, (double) activityCount);
                }
            }

            if (!userActivityCounts.isEmpty()) {
                rankingService.updateScoresBatch(RankingType.ACTIVITY_COUNT, userActivityCounts);
                log.debug("[RankingSync] Synced {} users' activity counts (page {}/{})",
                    userActivityCounts.size(), page + 1, totalPages);
            }
        }

        log.info("[RankingSync] Activity count ranking sync completed");
    }

    /**
     * 특정 유저의 순위 정보 동기화
     *
     * @param userId 동기화할 유저 ID
     */
    @Transactional(readOnly = true)
    public void syncUserRanking(Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElse(null);

            if (user == null) {
                log.warn("[RankingSync] User not found for sync: userId={}", userId);
                return;
            }

            // 포인트 순위 업데이트
            if (user.getPoints() > 0) {
                rankingService.updatePointsRanking(userId, user.getPoints());
            } else {
                // 0점이면 순위에서 제거
                rankingService.removeUser(RankingType.POINTS, userId);
            }

            // 활동량 순위 업데이트
            long activityCount = userActivityRepository.countByUserId(userId);
            if (activityCount > 0) {
                rankingService.updateActivityCountRanking(userId, activityCount);
            } else {
                // 활동이 없으면 순위에서 제거
                rankingService.removeUser(RankingType.ACTIVITY_COUNT, userId);
            }

            log.debug("[RankingSync] User ranking synced. userId={}, points={}, activities={}",
                userId, user.getPoints(), activityCount);

        } catch (Exception e) {
            log.error("[RankingSync] Failed to sync user ranking. userId={}", userId, e);
        }
    }

    /**
     * 증분 동기화 - 최근 활동이 있는 유저들만 동기화
     * 스케줄러에서 주기적으로 호출
     */
    @Transactional(readOnly = true)
    public void syncRecentlyActiveUsers() {
        log.info("[RankingSync] Starting incremental sync for recently active users");

        try {
            // 최근 24시간 이내 활동이 있는 유저 조회
            List<Long> activeUserIds = userActivityRepository.findRecentlyActiveUserIds(24);

            if (activeUserIds.isEmpty()) {
                log.info("[RankingSync] No recently active users to sync");
                return;
            }

            log.info("[RankingSync] Syncing {} recently active users", activeUserIds.size());

            // 배치 처리를 위한 맵 생성
            Map<Long, Double> pointsMap = new HashMap<>();
            Map<Long, Double> activityMap = new HashMap<>();

            for (Long userId : activeUserIds) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getPoints() > 0) {
                    pointsMap.put(userId, (double) user.getPoints());
                }

                long activityCount = userActivityRepository.countByUserId(userId);
                if (activityCount > 0) {
                    activityMap.put(userId, (double) activityCount);
                }
            }

            // 배치 업데이트
            if (!pointsMap.isEmpty()) {
                rankingService.updateScoresBatch(RankingType.POINTS, pointsMap);
            }
            if (!activityMap.isEmpty()) {
                rankingService.updateScoresBatch(RankingType.ACTIVITY_COUNT, activityMap);
            }

            log.info("[RankingSync] Incremental sync completed. Points: {}, Activities: {}",
                pointsMap.size(), activityMap.size());

        } catch (Exception e) {
            log.error("[RankingSync] Failed to sync recently active users", e);
        }
    }
}
