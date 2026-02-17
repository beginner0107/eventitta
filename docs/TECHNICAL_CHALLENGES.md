# 기술적 챌린지 & 해결 과정

## 1. JWT 인증 보안 설계

### 문제 상황

JWT 기반 인증에서 **토큰 저장 위치**와 **Refresh Token 탈취** 시나리오를 고려해야 했습니다.

- localStorage 저장 시 XSS를 통해 JavaScript로 토큰 접근 가능
- Refresh Token이 탈취되면 장기간 인증이 유지됨

### 해결 방법

**1. HttpOnly 쿠키 기반 토큰 전달**

```java
// CookieUtil.java
public static ResponseCookie createAccessTokenCookie(
    String accessToken, long validityMs) {
    return ResponseCookie.from(ACCESS_TOKEN, accessToken)
        .httpOnly(true)           // JavaScript 접근 차단
        .sameSite("Strict")       // CSRF 방어
        .path("/")
        .maxAge(Duration.ofMillis(validityMs))
        .build();
}
```

- `httpOnly(true)`: XSS로 JavaScript에서 쿠키 접근 불가
- `sameSite("Strict")`: Cross-site 요청에 쿠키 미전송 → CSRF 방어

**2. Refresh Token은 PBKDF2로 해시하여 저장**

```java
// TokenService.java
private void persistRefreshToken(Long userId, String rawRt) {
    String hash = pbkdf2PasswordEncoder.encode(rawRt);
    Instant expiresAt = tokenProvider.getRefreshTokenExpiry();
    User u = userRepository.getReferenceById(userId);
    refreshTokenRepository.save(new RefreshToken(u, hash, expiresAt));
}
```

- DB에는 해시값만 저장 → DB 유출 시에도 원본 토큰 복원 불가
- 비밀번호(`BCryptPasswordEncoder`)와 Refresh Token(`Pbkdf2PasswordEncoder`)의 인코더를 분리
- **PBKDF2 선택 이유**: Refresh Token은 이미 고엔트로피 랜덤값이므로 dictionary attack 방어보다 비교 효율이 중요. BCrypt는 72바이트 입력 제한이 있어 긴 토큰에서 잘림 가능성 존재

**3. 만료된 Refresh Token 정기 삭제**

```java
// RefreshTokenCleanupTask.java
@Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")  // 매 1시간
@SchedulerLock(name = "cleanUpExpiredRT", lockAtMostFor = "PT10M")
```

### 핵심 판단

| 저장 방식 | XSS 방어 | CSRF 방어 | 선택 |
|-----------|----------|-----------|------|
| localStorage | ❌ | ✅ | - |
| Cookie (httpOnly + SameSite) | ✅ | ✅ | ✅ |

---

## 2. 동시성 제어: 상황별 전략 분리

### 문제 상황

두 가지 동시성 이슈가 있었고, 각각 다른 특성을 가지고 있었습니다:

1. **모임 참가 승인**: 정원 확인 → 승인 (Check-Then-Act, 여러 필드 변경)
2. **포인트 증감**: 단순한 숫자 연산 (Set 연산)

### 해결 방법

**1. 모임 참가 승인 — 비관적 락**

정원을 읽고, 검증하고, 멤버 수를 증가시키는 Check-Then-Act 패턴입니다.
읽은 시점과 쓰는 시점 사이에 다른 트랜잭션이 동일 모임을 수정하면 정원 초과가 발생합니다.

```java
// MeetingRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({
    @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
})
@Query("SELECT m FROM Meeting m WHERE m.id = :id")
Optional<Meeting> findByIdForUpdate(@Param("id") Long id);
```

```java
// MeetingService.approveParticipant()
Meeting meeting = meetingRepository.findByIdForUpdate(meetingId);  // 락 획득
validateMeetingCapacity(meeting);                                  // 정원 검증
participant.approve();
meeting.incrementCurrentMembers();                                 // 인원 증가
```

- `lock.timeout = 3000ms`: 대기 시간 제한으로 데드락 방지

**2. 포인트 증감 — Atomic Update**

포인트 증감은 "현재 값을 읽어서 비교"할 필요 없이, 단순히 더하거나 빼는 연산입니다.

```java
// UserRepository.java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE User u SET u.points = u.points + :amount WHERE u.id = :userId")
int incrementPoints(@Param("userId") Long userId, @Param("amount") int amount);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE User u SET u.points = u.points - :amount " +
    "WHERE u.id = :userId AND u.points >= :amount")
int decrementPoints(@Param("userId") Long userId, @Param("amount") int amount);
```

- `WHERE u.points >= :amount`: SQL 레벨에서 포인트 부족 검증까지 원자적으로 처리
- `clearAutomatically = true`: 영속성 컨텍스트와 DB 값의 불일치 방지

### 왜 낙관적 락을 선택하지 않았는가

| 전략 | 모임 참가 | 포인트 증감 |
|------|-----------|-------------|
| 비관적 락 | ✅ 선택 | 과도 |
| 낙관적 락 | 정원 초과 후 재시도 필요 | `@Version` 추가 필요, 장점 없음 |
| Atomic Update | 불가 (검증 로직 포함) | ✅ 선택 (가장 단순) |

---

## 3. 이벤트 기반 아키텍처 진화

### 문제의 시작: 강결합

활동 기록 시 뱃지 체크, 랭킹 업데이트, 포인트 부여를 모두 한 트랜잭션에서 처리하면:
- 뱃지 체크 실패 → 전체 롤백 (핵심 기능과 부가 기능이 결합)
- 트랜잭션 길어짐 → 락 보유 시간 증가 → **데드락 발생**

### 진화 과정

**1단계: Spring Events + @Async로 분리**

핵심 데이터(활동 기록 + 포인트)만 트랜잭션에 남기고, 부가 작업은 이벤트로 분리:

```java
// UserActivityService.recordActivity()
@Transactional
public void recordActivity(Long userId, ActivityType activityType, Long targetId) {
    // 1. 활동 기록 저장
    UserActivity userActivity = createUserActivity(userId, activityType, targetId);
    userActivityRepository.save(userActivity);

    // 2. 포인트 Atomic Update
    int points = activityType.getDefaultPoint();
    if (points > 0) {
        userRepository.incrementPoints(userId, points);
    }

    // 3. 이벤트 발행 → 뱃지/랭킹은 비동기 처리
    eventPublisher.publishEvent(new ActivityRecordedEvent(...));
}
```

```java
// ActivityPostProcessor.java — 트랜잭션 커밋 후 비동기 처리
@Async("gamificationExecutor")
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handleActivityRecorded(ActivityRecordedEvent event) {
    CompletableFuture<Void> badgeFuture = processBadgesAsync(event);
    CompletableFuture<Void> rankingFuture = processRankingsAsync(event);
    CompletableFuture.allOf(badgeFuture, rankingFuture)...
}
```

**성과**: 뱃지/랭킹 실패가 핵심 트랜잭션에 영향을 주지 않음

**2단계: 실패 복구 — @Retryable + FailedActivityEvent**

비동기 이벤트는 실패 시 유실됩니다. 이를 방지하기 위해:

```java
// UserActivityEventListener.java
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
@Retryable(retryFor = Exception.class, maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2))
public void handleUserActivity(UserActivityLogRequestedEvent event) { ... }

@Recover
public void recoverUserActivity(Exception e, UserActivityLogRequestedEvent event) {
    // 최종 실패 시 DB에 저장 (FailedActivityEvent 테이블)
    failedEventRepository.save(FailedActivityEvent.from(event, e.getMessage()));
    // Discord 알림
    discordNotificationService.sendAlert(...);
}
```

실패 이벤트는 스케줄러가 주기적으로 재처리:

```java
// FailedActivityEventRetryScheduler.java
@Scheduled(fixedDelay = FAILED_EVENT_RETRY_FIXED_DELAY_MS)
@SchedulerLock(name = "retryFailedActivityEvents", lockAtMostFor = "PT55S")
@Transactional(propagation = NEVER)  // 스케줄러 자체는 트랜잭션 없이
public void retryFailedEvents() {
    for (FailedActivityEvent event : eventsToProcess) {
        // 개별 독립 트랜잭션으로 처리
        failedEventRecoveryService.recoverFailedEventIndependently(event.getId());
    }
}
```

```java
// FailedEventRecoveryService.java — 개별 트랜잭션 격리
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void recoverFailedEventIndependently(Long eventId) {
    FailedActivityEvent event = failedEventRepository.findByIdWithLock(eventId);
    event.markAsProcessing();            // 상태 전환 (동시 처리 방지)
    failedEventRepository.saveAndFlush(event);
    executeRecovery(event);              // 실제 복구
    event.markAsProcessed();
}
```

**3단계: Transactional Outbox 패턴**

Spring Events의 근본 한계 — 이벤트 발행과 비즈니스 데이터 저장이 원자적이지 않음 — 을 해결하기 위해 Outbox 패턴을 도입:

```java
// ActivityOutboxWriter.java — 비즈니스 트랜잭션과 같은 트랜잭션에서 Outbox INSERT
@Transactional
public void write(ActivityType activityType, Long userId, Long targetId,
        OperationType operationType) {
    ActivityOutbox outbox = ActivityOutbox.builder()
            .idempotencyKey(generateIdempotencyKey(...))
            .userId(userId)
            .activityType(activityType)
            .operationType(operationType)
            .targetId(targetId)
            .build();
    activityOutboxRepository.save(outbox);
}
```

```java
// OutboxRelayService.java — 독립 트랜잭션으로 Outbox 레코드 처리
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void processIndependently(Long outboxId) {
    ActivityOutbox outbox = outboxRepository.findByIdWithLock(outboxId);
    outbox.markAsProcessing();
    outboxRepository.saveAndFlush(outbox);

    executeActivity(outbox);    // RECORD or REVOKE
    outbox.markAsDone();
}
```

Stuck 레코드 복구 및 완료 레코드 정리까지 포함:

```java
// OutboxRelayScheduler.java
@Scheduled(fixedDelay = OUTBOX_RELAY_FIXED_DELAY_MS)       // Outbox 폴링
@Scheduled(fixedDelay = STUCK_PROCESSING_RECOVERY_FIXED_DELAY_MS)  // Stuck 복구
@Scheduled(cron = "0 0 4 * * *")                           // 하우스키핑
```

### 아키텍처 요약

```
비즈니스 로직 (MeetingService, PostService 등)
    │
    ├── ActivityOutboxWriter.write()     ← 같은 트랜잭션
    │
    └── [트랜잭션 커밋]
              │
    OutboxRelayScheduler (폴링)
              │
    OutboxRelayService.processIndependently()
              │
    UserActivityService.recordActivity()
         │
         ├── 활동 저장 + 포인트 Atomic Update
         │
         └── ActivityRecordedEvent 발행
                    │
              ActivityPostProcessor (비동기)
                    ├── 뱃지 체크
                    └── 랭킹 업데이트 (Redis)
```

---

## 4. N+1 문제 해결과 QueryDSL 동적 쿼리

### 문제 상황

게시글 목록 조회 시 Post → User, Post → Region이 N+1로 조회되었습니다.
`@EntityGraph`로 해결할 수도 있지만, 검색 조건(제목/내용/지역)이 동적으로 조합되어야 했습니다.

### 해결 방법

**1. fetchJoin + 동적 필터 (엔티티 조회)**

```java
// PostRepositoryImpl.java
@Override
public Page<Post> findAllByFilter(PostFilter filter, Pageable pageable) {
    BooleanBuilder predicate = buildFilter(filter);

    List<Post> content = queryFactory
        .selectFrom(post)
        .join(post.user, postUser).fetchJoin()      // N+1 해결
        .join(post.region, postRegion).fetchJoin()   // N+1 해결
        .where(predicate)
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(post.createdAt.desc())
        .fetch();

    return PageableExecutionUtils.getPage(content, pageable, ...);
}
```

**2. Projection DTO (필요한 컬럼만 조회)**

목록 화면에서 게시글 전체 내용은 불필요합니다. `Projections.constructor`로 필요한 컬럼만 조회:

```java
// PostRepositoryImpl.findSummaries()
List<PostSummaryResponse> content = queryFactory
    .select(Projections.constructor(
        PostSummaryResponse.class,
        post.id, post.title, post.user.nickname,
        post.region.code, post.likeCount, post.createdAt
    ))
    .from(post)
    .join(post.user, user)
    .join(post.region, region)
    .where(predicate)
    ...
```

### 핵심 판단

| 방법 | 동적 필터 | N+1 해결 | 컬럼 선택 |
|------|-----------|----------|-----------|
| `@EntityGraph` | ❌ | ✅ | ❌ |
| JPQL + fetchJoin | 제한적 | ✅ | ❌ |
| **QueryDSL** | ✅ | ✅ | ✅ |

---

## 5. Festival 거리 검색 최적화

### 문제 상황

사용자 위치(위도, 경도) 기준으로 반경 N km 이내 축제를 검색하려면 Haversine 공식으로 모든 행과의 거리를 계산해야 합니다. 이 경우 **Full Table Scan + 수학 연산**이 행 수만큼 발생합니다.

### 해결 방법: 2단계 필터링

**1단계: Bounding Box로 1차 필터링 (인덱스 활용)**

정사각형 영역을 계산하여 인덱스 기반으로 대부분의 행을 걸러냅니다:

```java
// BoundingBoxCalculator.java
public BoundingBox calculate(double latitude, double longitude, double distanceKm) {
    double latDelta = distanceKm / KM_PER_DEGREE_LAT;  // 위도 1도 ≈ 111km
    double lonDelta = distanceKm / (KM_PER_DEGREE_LAT * Math.cos(Math.toRadians(latitude)));

    return new BoundingBox(
        latitude - latDelta,    // 남쪽 경계
        latitude + latDelta,    // 북쪽 경계
        longitude - lonDelta,   // 서쪽 경계
        longitude + lonDelta    // 동쪽 경계
    );
}
```

**2단계: Haversine 공식으로 정확한 거리 계산**

1차 필터링으로 좁혀진 후보에만 Haversine 공식을 적용합니다.

```
WHERE latitude BETWEEN :minLat AND :maxLat           ← 인덱스 스캔
  AND longitude BETWEEN :minLon AND :maxLon          ← 인덱스 스캔
  AND haversine(:lat, :lon, latitude, longitude) <= :distance  ← 후보에만 적용
```

### 왜 Spatial Index를 쓰지 않았는가

| 방식 | 장점 | 단점 |
|------|------|------|
| Spatial Index (R-Tree) | 최적의 성능 | MySQL 버전/엔진 제약, 스키마 마이그레이션 |
| **Bounding Box + Haversine** | 인덱스만으로 충분, 구현 단순 | Spatial에 비해 약간 느림 |

현재 데이터 규모에서는 Bounding Box 방식으로 충분한 성능을 얻었고, 복합 인덱스 `(latitude, longitude)`로 인덱스 스캔이 가능하여 추가 최적화 없이 목표 성능을 달성했습니다.

---

## 6. Redis 실시간 랭킹 + MySQL Fallback

### 문제 상황

포인트 랭킹 조회 시 매번 `ORDER BY points DESC`로 정렬하면 데이터 증가에 따라 성능이 저하됩니다. 포인트가 변경될 때마다 실시간으로 순위에 반영되어야 합니다.

### 해결 방법

**Redis Sorted Set으로 O(log N) 랭킹 업데이트 + 조회:**

```java
// RedisRankingService.java
@Override
public void updatePointsRanking(Long userId, int points) {
    try {
        redisTemplate.opsForZSet().add(
            RankingType.POINTS.getRedisKey(),
            userId.toString(),
            points
        );
    } catch (Exception e) {
        log.error("Failed to update points ranking. userId={}", userId, e);
    }
}
```

**Redis 장애 시 MySQL Fallback:**

```java
@Override
@Transactional(readOnly = true)
public RankingPageResponse getTopRankings(RankingType type, int limit) {
    try {
        return getTopRankingsFromRedis(type, limit);
    } catch (Exception e) {
        log.error("Redis failed, fallback to MySQL. type={}, error={}", type, e.getMessage());
        return getTopRankingsFromDatabase(type, limit);
    }
}
```

MySQL Fallback 쿼리:

```java
// UserRepository.java
@Query("SELECT u FROM User u WHERE u.deleted = false ORDER BY u.points DESC")
List<User> findTopUsersByPoints(Pageable pageable);
```

### 한계와 트레이드오프

- Fallback이 `try-catch` 기반이므로, Redis가 느리게 응답하는 경우(timeout) 사용자 경험 저하 가능
- Circuit Breaker 패턴을 적용하면 더 빠른 전환이 가능하지만, 현재 규모에서는 `try-catch` 수준으로 충분하다고 판단

---

## 7. 배치 실패 격리

### 문제 상황

스케줄러에서 여러 레코드를 순차 처리할 때, 하나의 레코드 실패가 전체 배치를 롤백시키는 문제:

```
[레코드 1] 성공
[레코드 2] 실패 → 전체 롤백 → 레코드 1도 취소됨
```

### 해결 방법

**스케줄러는 트랜잭션 없이, 각 레코드는 독립 트랜잭션으로 처리:**

```java
// OutboxRelayScheduler.java
@Scheduled(fixedDelay = OUTBOX_RELAY_FIXED_DELAY_MS)
@Transactional(propagation = NEVER)          // 스케줄러 자체는 트랜잭션 X
public void relay() {
    for (ActivityOutbox outbox : pendingEvents) {
        try {
            outboxRelayService.processIndependently(outbox.getId());  // REQUIRES_NEW
            successCount++;
        } catch (Exception e) {
            failureCount++;  // 하나가 실패해도 다음 레코드 계속 처리
        }
    }
}
```

```java
// OutboxRelayService.java
@Transactional(propagation = Propagation.REQUIRES_NEW)   // 각각 독립 트랜잭션
public void processIndependently(Long outboxId) { ... }
```

이 패턴은 `FailedEventRecoveryService`, `OutboxRelayService` 두 곳 모두에 동일하게 적용됩니다.

### 핵심

| | 단일 트랜잭션 | 개별 트랜잭션 (REQUIRES_NEW) |
|-|---------------|------------------------------|
| 레코드 1 성공 + 레코드 2 실패 | 전체 롤백 | 레코드 1 커밋, 레코드 2만 롤백 |
| 성능 | 커넥션 1개 | 커넥션 N개 |
| 적용 조건 | 전부 성공 or 전부 실패 | **부분 실패 허용** |
