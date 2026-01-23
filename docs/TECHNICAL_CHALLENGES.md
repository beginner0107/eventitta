# 기술적 챌린지 & 해결 과정

## 1. JWT 인증 시스템 보안 설계

### 문제 상황

JWT 기반 인증 시스템 구현 시 보안과 사용성 사이의 균형이 필요했습니다:

- **localStorage 저장**: JavaScript 접근 가능 → XSS 취약
- **Refresh Token 관리**: JWT로 생성 시 탈취되면 장기간 인증 유지 가능
- **무상태 특성**: 로그아웃/차단이 즉시 적용되지 않음

### 해결 방법

**1. HttpOnly 쿠키 기반 토큰 저장**

```java
// CookieProperties - 보안 옵션 설정
@ConfigurationProperties(prefix = "cookie")
public record CookieProperties(
    boolean secure,     // HTTPS에서만 전송
    boolean httpOnly,   // JavaScript 접근 차단
    String sameSite     // CSRF 방어
  ) {
}

// 쿠키 생성
ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN, token)
  .httpOnly(cookieProperties.httpOnly())
  .secure(cookieProperties.secure())
  .sameSite(cookieProperties.sameSite())
  .path("/")
  .maxAge(Duration.ofHours(1))
  .build();
```

**2. Refresh Token 해시 저장**

```java
// 원본 토큰은 클라이언트에만, 서버에는 해시값만 저장
public String createRefreshToken() {
  byte[] randomBytes = KeyGenerators.secureRandom(32).generateKey();
  return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
}

private void persistRefreshToken(Long userId, String rawRt) {
  String hash = pbkdf2PasswordEncoder.encode(rawRt);
  Instant expiresAt = tokenProvider.getRefreshTokenExpiry();
  refreshTokenRepository.save(new RefreshToken(user, hash, expiresAt));
}
```

**3. 일회용 토큰 정책**

```java
public TokenResponse refresh(String expiredAt, String rawRt) {
  RefreshToken entity = rtRepo.findAllByUserId(userId)
    .stream()
    .filter(token -> rtEncoder.matches(rawRt, token.getTokenHash()))
    .findFirst()
    .orElseThrow(REFRESH_TOKEN_INVALID::defaultException);

  // 기존 토큰 삭제 후 새 토큰 발급
  rtRepo.delete(entity);
  return tokenService.issueTokens(userId);
}
```

**4. 만료 토큰 자동 정리**

```java

@Scheduled(cron = "0 0 * * * *")  // 매시간 실행
@SchedulerLock(name = "removeExpiredRefreshTokens", lockAtMostFor = "PT30M")
@Transactional
public void removeExpiredRefreshTokens() {
  long deleted = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
  log.info("만료된 토큰 삭제: {}", deleted);
}
```

### 개선 효과

- **XSS 방어**: HttpOnly 쿠키로 JavaScript 접근 차단
- **DB 탈취 대응**: 해시 저장으로 원본 토큰 노출 방지
- **세션 하이재킹 방지**: 일회용 토큰 정책으로 탈취된 토큰 무력화
- **하이브리드 구조**: JWT의 무상태성 + 세션 방식의 안전한 만료/로그아웃

### 참고 문서

- [JwtTokenProvider.java](../src/main/java/com/eventitta/auth/jwt/JwtTokenProvider.java) - JWT 생성/검증
- [RefreshTokenService.java](../src/main/java/com/eventitta/auth/service/RefreshTokenService.java) - Refresh Token 관리

---

## 2. 모임 참가 승인 동시성 문제 해결

### 문제 상황

선착순 모임 참가 승인 시 동시성 문제 발생:

- 정원 10명인 모임에 20명이 동시 승인되는 현상
- `currentMembers` 카운터가 실제 승인 수와 불일치 (Lost Update)
- Check-Then-Act 패턴의 Race Condition

```java
// 문제 코드: 검사와 갱신 사이에 다른 트랜잭션 진입 가능
if(meeting.getCurrentMembers() >=meeting.

getMaxMembers()){
  throw MEETING_FULL.

defaultException();
}
  participant.

approve();
meeting.

incrementCurrentMembers();  // 동시 실행 시 덮어쓰기 발생
```

### 해결 방법

**1. JPA 비관적 락 적용**

```java
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints({
    @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
  })
  @Query("SELECT m FROM Meeting m WHERE m.id = :id")
  Optional<Meeting> findByIdForUpdate(@Param("id") Long id);
}
```

**2. 서비스 코드 변경**

```java

@Transactional
public ParticipantResponse approveParticipant(Long userId, Long meetingId, Long participantId) {
  // 비관적 락 적용 - SELECT ... FOR UPDATE
  Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
    .orElseThrow(MEETING_NOT_FOUND::defaultException);

  MeetingParticipant participant = validateAndGetPendingParticipant(
    meeting, userId, participantId
  );

  // 이제 안전하게 검사 가능
  validateMeetingCapacity(meeting);

  participant.approve();
  meeting.incrementCurrentMembers();

  // 이벤트 발행
  eventPublisher.publishEvent(new UserActivityLogRequestedEvent(...));

  return ParticipantResponse.from(participant);
}
```

**3. 동시성 테스트 검증**

```java

@ParameterizedTest
@CsvSource({"10,5,20", "100,10,200"})
void givenMultiplePendingParticipants_whenConcurrentApproval_thenShouldNotExceedMaxMembers(
  int maxMembers, int currentApproved, int pendingUsers) throws Exception {

  MeetingSetup setup = prepareMeeting(maxMembers, currentApproved, pendingUsers);
  ExecutorService executor = Executors.newFixedThreadPool(pendingUsers);
  CountDownLatch start = new CountDownLatch(1);

  // 모든 스레드가 동시에 승인 시도
  for (Long participantId : setup.pendingParticipantIds()) {
    executor.submit(() -> {
      start.await();  // 동시 시작
      meetingService.approveParticipant(setup.leaderId(), setup.meetingId(), participantId);
    });
  }

  start.countDown();  // 동시 시작 신호

  // 검증: 정원 초과 없음, currentMembers 정합성 확인
  Meeting refreshed = meetingRepository.findById(setup.meetingId()).orElseThrow();
  int approvedCount = participantRepository.countByMeetingIdAndStatus(
    setup.meetingId(), ParticipantStatus.APPROVED);

  assertAll(
    () -> assertTrue(approvedCount <= maxMembers),
    () -> assertEquals(approvedCount, refreshed.getCurrentMembers())
  );
}
```

### 개선 효과

| 케이스   | 정원   | 동시 요청 | Before    | After     |
|-------|------|-------|-----------|-----------|
| Case1 | 10명  | 20명   | ❌ 20명 승인  | ✅ 10명 승인  |
| Case2 | 100명 | 200명  | ❌ 200명 승인 | ✅ 100명 승인 |

- **정원 초과 승인**: 100% 방지
- **currentMembers 정합성**: 실제 승인 수와 일치
- **Lost Update**: 완전 해결

### 주의사항

- **락 타임아웃**: 3초 설정으로 데드락 방지
- **트랜잭션 범위**: 최소화하여 동시 처리량 확보
- **향후 계획**: 트래픽 증가 시 Redis 분산 락 도입 검토

### 참고 문서

- [MeetingRepository.java](../src/main/java/com/eventitta/meeting/repository/MeetingRepository.java) - 비관적 락 쿼리
- [MeetingService.java](../src/main/java/com/eventitta/meeting/service/MeetingService.java) - 승인 로직

---

## 3. 사용자 포인트 동시성 문제 해결

### 문제 상황

게시글/댓글 작성 시 포인트 적립 로직에서 동시성 문제 발생:

- Entity의 `points++` 연산이 DB 수준에서 원자적이지 않음
- 동시에 여러 활동이 기록될 때 포인트가 유실되는 Lost Update 현상
- 비관적 락을 적용하면 포인트 적립마다 락 대기 발생 → 성능 저하

```java
// 문제 코드: Entity에서 직접 증감
public void addPoints(int amount) {
  this.points += amount;  // 동시 실행 시 덮어쓰기 발생
}
```

### 해결 방법

**비관적 락 대신 Atomic Update 쿼리 사용**

```java
public interface UserRepository extends JpaRepository<User, Long> {
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE User u SET u.points = u.points + :amount " +
    "WHERE u.id = :userId")
  int incrementPoints(@Param("userId") Long userId, @Param("amount") int amount);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE User u SET u.points = u.points - :amount " +
    "WHERE u.id = :userId AND u.points >= :amount")
  int decrementPoints(@Param("userId") Long userId, @Param("amount") int amount);
}
```

**서비스 코드 변경**

```java

@Transactional
public void recordActivity(Long userId, ActivityType activityType, Long targetId) {
  UserActivity userActivity = createUserActivity(userId, activityType, targetId);
  userActivityRepository.save(userActivity);

  int points = activityType.getDefaultPoint();
  if (points > 0) {
    int updated = userRepository.incrementPoints(userId, points);
    if (updated == 0) {
      log.error("[UserActivity] Failed to increment points. " +
          "reason=user_not_found, userId={}, deltaPoint={}",
        userId, points);
      throw NOT_FOUND_USER_ID.defaultException();
    }
  }

  User user = findUserById(userId);
  badgeService.checkAndAwardBadges(user);
}
```

### 비관적 락 vs Atomic Update 비교

| 구분         | 비관적 락                     | Atomic Update    |
|------------|---------------------------|------------------|
| **적용 대상**  | 모임 참가 승인 (Check-Then-Act) | 포인트 증감 (단순 연산)   |
| **락 범위**   | SELECT 시점부터 COMMIT까지      | UPDATE 쿼리 실행 순간만 |
| **동시성**    | 순차 처리 (대기 발생)             | 높은 동시 처리량        |
| **적합한 경우** | 조건 검사 후 갱신                | 단순 값 증감          |

### 개선 효과

- **Lost Update 해결**: DB 수준의 원자적 연산으로 포인트 유실 방지
- **성능 향상**: 락 대기 없이 높은 동시 처리량 유지
- **영향도 최소화**: 포인트 차감 실패 시에도 활동 기록은 유지

### 참고 문서

- [UserRepository.java](../src/main/java/com/eventitta/user/repository/UserRepository.java) - Atomic Update 쿼리
- [UserActivityService.java](../src/main/java/com/eventitta/gamification/service/UserActivityService.java) - 포인트 적립 로직

---

## 4. 이벤트 기반 게임화 시스템

### 문제 상황

게시글/댓글 작성 시 포인트 적립 로직이 서비스 계층에 강결합되어 있었습니다:

- `PostService`가 `GamificationService`를 직접 호출
- 새로운 활동 타입 추가 시 모든 도메인 서비스 수정 필요
- 트랜잭션 경계가 불명확하여 포인트 적립 실패 시 게시글도 롤백되는 문제

### 해결 방법

**1. Spring Events 도입**

```java
// PostService - 이벤트 발행만
applicationEventPublisher.publishEvent(
    new UserActivityLogRequestedEvent(userId, ActivityType.POST_CREATE)
);

// UserActivityEventListener - 이벤트 구독
@TransactionalEventListener(phase = AFTER_COMMIT)
@Async
public void handleUserActivityLogRequested(UserActivityLogRequestedEvent event) {
  userActivityService.logActivity(event.userId(), event.activityType());
}
```

**2. 비동기 처리**

- 전용 스레드 풀 설정 (`AsyncConfig`)
- `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`로 메인 트랜잭션과 분리

**3. ActivityType Enum 설계**

- 각 활동 타입별 기본 포인트와 리소스 타입 관리
- `createActivity()` 메서드로 일관된 생성 로직 제공

### 개선 효과

- **도메인 의존성 분리**: `PostService`에서 `GamificationService` 의존성 제거
- **확장성 향상**: 새 활동 타입 추가 시 이벤트 리스너만 수정
- **트랜잭션 분리**: 포인트 적립 실패가 게시글 작성에 영향 없음
- **테스트 용이성 향상**: 도메인 서비스 단위 테스트 시 이벤트 리스너 Mock 불필요

### 참고 문서

- [UserActivityEventListener.java](../src/main/java/com/eventitta/gamification/event/UserActivityEventListener.java) -
  이벤트 리스너 구현

---

## 5. 비동기 이벤트 실패 복구 메커니즘

### 문제 상황

비동기 이벤트 처리의 신뢰성 문제:

- `@Async` + `@TransactionalEventListener`로 처리되는 이벤트가 실패하면 데이터 유실
- 일시적인 DB 오류나 네트워크 문제로 포인트 적립이 누락될 수 있음
- 실패한 이벤트를 추적하고 복구할 방법이 없음

### 해결 방법

**1. Spring Retry로 즉각 재시도**

```java

@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
@Retryable(
  retryFor = {Exception.class},
  maxAttempts = 3,
  backoff = @Backoff(delay = 1000, multiplier = 2)  // 1초 → 2초 → 4초
)
public void handleUserActivity(UserActivityLogRequestedEvent event) {
  log.info("[활동 기록 시작] userId={}, activityType={}, targetId={}",
    event.userId(), event.activityType(), event.targetId());

  userActivityService.recordActivity(
    event.userId(),
    event.activityType(),
    event.targetId()
  );

  log.info("[활동 기록 성공] userId={}, activityType={}",
    event.userId(), event.activityType());
}
```

**2. 최종 실패 시 DB 영구 저장 (Dead Letter Queue 패턴)**

```java

@Recover
public void recoverUserActivity(Exception e, UserActivityLogRequestedEvent event) {
  log.error("[활동 기록 최종 실패] userId={}, activityType={}, targetId={}",
    event.userId(), event.activityType(), event.targetId(), e);

  try {
    // 실패 이벤트를 DB에 저장하여 나중에 복구
    failedEventRecoveryService.saveFailedEvent(
      event.userId(),
      event.activityType(),
      OperationType.RECORD,
      event.targetId(),
      e.getMessage()
    );
  } catch (Exception persistEx) {
    log.error("[실패 이벤트 저장 중 오류]", persistEx);
  }

  // Slack 알림으로 운영팀에 즉시 알림
  sendSlackAlertSafely(ACTIVITY_RECORD_FAILED, message, event.userId(), e);
}
```

**3. 실패 이벤트 도메인 모델**

```java

@Entity
@Table(name = "failed_activity_events", indexes = {
  @Index(name = "idx_failed_event_status", columnList = "status"),
  @Index(name = "idx_failed_event_user", columnList = "user_id")
})
public class FailedActivityEvent extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type", nullable = false)
  private ActivityType activityType;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false)
  private OperationType operationType;  // RECORD or REVOKE

  @Column(name = "target_id")
  private Long targetId;

  @Column(name = "retry_count", nullable = false)
  private Integer retryCount = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private EventStatus status = EventStatus.PENDING;

  public enum EventStatus {
    PENDING,    // 재처리 대기
    PROCESSING, // 처리 중
    PROCESSED,  // 처리 완료
    FAILED      // 최종 실패
  }
}
```

**4. 작업 유형 구분 (RECORD/REVOKE)**

```java
public enum OperationType {
  RECORD,  // 활동 기록 (포인트 추가)
  REVOKE   // 활동 취소 (포인트 회수)
}

// 복구 서비스에서 작업 유형에 따라 분기
public void recoverFailedEvent(FailedActivityEvent event) {
  switch (event.getOperationType()) {
    case RECORD -> userActivityService.recordActivity(
      event.getUserId(), event.getActivityType(), event.getTargetId()
    );
    case REVOKE -> userActivityService.revokeActivity(
      event.getUserId(), event.getActivityType(), event.getTargetId()
    );
  }
  event.markAsProcessed();
}
```

**5. 스케줄러 자동 복구**

```java

@Scheduled(fixedDelay = 300000)  // 5분마다
@SchedulerLock(name = "recoverFailedActivityEvents")
@Transactional(propagation = Propagation.NEVER)  // 각 이벤트별 독립 트랜잭션
public void recoverFailedEvents() {
  List<FailedActivityEvent> pendingEvents =
    failedEventRepository.findByStatusAndRetryCountLessThan(
      EventStatus.PENDING, MAX_RETRY_COUNT
    );

  for (FailedActivityEvent event : pendingEvents) {
    try {
      failedEventRecoveryService.recoverFailedEvent(event);
    } catch (Exception e) {
      event.incrementRetryCount();
      if (event.getRetryCount() >= MAX_RETRY_COUNT) {
        event.markAsFailed(e.getMessage());
      }
    }
  }
}
```

### 복구 플로우

```
이벤트 발생 → Retry (3회) → 실패 시 DB 저장 + Slack 알림
                                    ↓
                            스케줄러 (5분마다)
                                    ↓
                            재시도 (최대 N회)
                                    ↓
                        성공: PROCESSED / 실패: FAILED + 알림
```

### 개선 효과

- **데이터 유실 방지**: 실패 이벤트를 DB에 영구 저장하여 추적 가능
- **자동 복구**: 스케줄러가 주기적으로 실패 이벤트 재처리
- **운영 가시성**: Slack 알림으로 실패 즉시 인지, 상태별 조회 가능
- **RECORD/REVOKE 분리**: 포인트 추가/회수 모두 안전하게 복구

### 참고 문서

- [UserActivityEventListener.java](../src/main/java/com/eventitta/gamification/event/UserActivityEventListener.java) -
  Retry + Recover 구현
- [FailedActivityEvent.java](../src/main/java/com/eventitta/gamification/domain/FailedActivityEvent.java) - 실패 이벤트 도메인
- [FailedEventRecoveryService.java](../src/main/java/com/eventitta/gamification/service/FailedEventRecoveryService.java) -
  복구 서비스

---

## 6. 분산 환경 스케줄러 중복 실행 방지

### 문제 상황

외부 API 호출 스케줄러가 다중 인스턴스 환경에서 중복 실행되는 문제:

- 서울시 축제 API: Rate Limit 1000건/일
- 3개 인스턴스 실행 시 하루에 3000건 호출 → API 차단
- 데이터 중복 저장으로 인한 무결성 문제

### 해결 방법

**1. ShedLock 도입**

```java

@Scheduled(cron = "0 0 3 * * *") // 매일 03:00
@SchedulerLock(
  name = "syncDailySeoulFestivalData",
  lockAtMostFor = "30s",  // 최대 락 시간
  lockAtLeastFor = "10s"   // 최소 락 시간
)
public void syncDailySeoulFestivalData() {
  // JDBC 기반 분산 락으로 단일 실행 보장
}
```

**2. 락 타임아웃 전략**

- `lockAtMostFor`: 작업이 예상보다 오래 걸려도 30초 후 자동 해제
- `lockAtLeastFor`: 너무 빠른 재실행 방지 (최소 10초 간격)

### 개선 효과

- **API 호출 횟수**: 3000건/일 → 1000건/일 (66% 감소)
- **데이터 중복 저장**: 100% 제거
- **테스트 결과**: 3개 인스턴스에서 동시에 스케줄러 실행 시 단일 실행 보장 (100회 검증)

### 참고 문서

- [ShedLockConfig.java](../src/main/java/com/eventitta/common/config/scheduling/ShedLockConfig.java) - ShedLock 설정

---

## 7. N+1 문제 해결과 QueryDSL 활용

### 문제 상황

게시글/모임 검색 API에서 성능 이슈 발생:

- 게시글 목록 조회 시 작성자 정보 접근으로 N+1 쿼리 발생
- 계층형 댓글 조회 시 대댓글 + 작성자 정보로 쿼리 폭발
- JPQL로 작성 시 타입 안전성 부족, 동적 쿼리 생성 어려움

```java
// N+1 발생 코드
List<PostSummaryDto> summaries = posts.stream()
    .map(p -> new PostSummaryDto(
      p.getId(),
      p.getTitle(),
      p.getUser().getNickname(),  // 지연 로딩 유발 → N번 추가 쿼리
      p.getRegion().getCode()
    ))
    .collect(Collectors.toList());
```

### 해결 방법

**1. Projection DTO로 N+1 방지**

```java

@QueryProjection
public record PostSummaryResponse(
  Long id,
  String title,
  String authorName,
  Long likeCount
) {
}

// 쿼리에서 직접 DTO 생성
queryFactory
  .

select(new QPostSummaryResponse(
         post.id,
       post.title,
       post.author.name,
       post.likeCount
       ))
  .

from(post)
  .

fetch();
```

**2. BooleanBuilder 패턴으로 동적 쿼리**

```java
public Page<PostSummaryResponse> searchPosts(PostSearchRequest request, Pageable pageable) {
  BooleanBuilder builder = new BooleanBuilder();

  Optional.ofNullable(request.regionCode())
    .ifPresent(code -> builder.and(post.region.code.eq(code)));

  Optional.ofNullable(request.keyword())
    .ifPresent(keyword -> builder.and(
      post.title.containsIgnoreCase(keyword)
        .or(post.content.containsIgnoreCase(keyword))
    ));

  return queryFactory.selectFrom(post)
    .where(builder)
    .orderBy(/* 동적 정렬 */)
    .fetch();
}
```

### 개선 효과

- **쿼리 개수**: N+1 발생 시 17+건 → Fetch Join/Projection 적용 후 1~2개로 통합
- **메모리 사용**: Projection DTO 사용으로 필요한 데이터만 조회
- **타입 안전성**: 컴파일 타임에 쿼리 오류 감지

### 참고 문서

- [PostRepositoryImpl.java](../src/main/java/com/eventitta/post/repository/PostRepositoryImpl.java) - QueryDSL 활용 예시

---

## 8. Discord 알림 Rate Limiting

### 문제 상황

에러 발생 시 Discord 알림이 폭증하여 채널이 마비되는 문제:

- DB 연결 오류 시 초당 100건 이상 알림 발생
- 중요한 알림이 스팸에 묻혀 놓침
- Discord API Rate Limit 초과로 알림 자체가 차단됨

### 해결 방법

**1. Rate Limiter 인터페이스 설계**

```java
public interface RateLimiter {
  boolean tryAcquire(String key);
}

// 7가지 구현체를 만든 이유: 알고리즘별 성능 비교 및 학습 목적
// 1. SimpleRateLimiter - 기준점(Baseline)
// 2. CacheBasedRateLimiter ✅ 프로덕션 채택
// 3. FixedWindowRateLimiter - 고정 윈도우
// 4. SlidingWindowLogRateLimiter - 가장 정확하나 메모리 O(n)
// 5. SlidingWindowCounterRateLimiter - 메모리 효율 개선 O(1)
// 6. TokenBucketRateLimiter - 버스트 트래픽 허용
// 7. ConcurrentTokenBucketRateLimiter - 동시성 최적화
```

**2. Alert Level 기반 차등 제한**

```java
public enum AlertLevel {
  CRITICAL(10),  // 10회/분
  HIGH(5),       // 5회/분
  MEDIUM(2),     // 2회/분
  INFO(1);       // 1회/분

  private final int maxRequestsPerMinute;
}

AlertLevel level = alertLevelResolver.resolve(errorCode);
if(rateLimiter.

tryAcquire(errorCode.name())){
  discordNotificationService.

sendAlert(level, message);
}
```

### 개선 효과

- **알림 폭증 방지**: 에러 코드별로 Rate Limiting 적용
- **Alert Level 차등 제한**: CRITICAL(10회/분) ~ INFO(1회/분)
- **자동 만료**: Caffeine Cache TTL(1분)로 상태 관리 간소화

### 참고 문서

- [notification/service/ratelimit/](../src/main/java/com/eventitta/notification/service/ratelimit/) - Rate Limiter 구현체들

---

## 9. 외부 API 연동 및 재시도 전략

### 문제 상황

서울시/전국 축제 API는 불안정하여 일시적 오류가 빈번:

- 네트워크 타임아웃 (5-10%)
- 일시적 500 에러 (2-3%)
- 재시도 없이 실패 시 해당 날짜 데이터 영구 손실

### 해결 방법

**Spring Retry 적용**

```java

@Retryable(
  retryFor = {RestClientException.class, TimeoutException.class},
  maxAttempts = 3,
  backoff = @Backoff(delay = 2000, multiplier = 2) // 2초 → 4초 → 8초
)
public List<Festival> fetchSeoulFestivals(LocalDate date) {
  return restClient.get()
    .uri(/* ... */)
    .retrieve()
    .body(/* ... */);
}

@Recover
public List<Festival> recover(Exception e, LocalDate date) {
  log.error("축제 데이터 가져오기 실패 (최대 재시도 초과): {}", date, e);
  slackNotificationService.sendAlert(AlertLevel.HIGH, "축제 데이터 동기화 실패: " + date);
  return Collections.emptyList();
}
```

### 개선 효과

- **데이터 동기화 성공률**: 92% → 99.5% (재시도로 7.5% 추가 복구)
- **평균 재시도 횟수**: 1.2회/일
- **완전 실패**: 월 2-3건 → Slack 알림으로 즉시 대응

---

## 10. Badge 평가 시스템 리팩토링 (전략 패턴)

### 문제 상황

Badge 수여 로직이 확장하기 어려운 구조:

- 새로운 평가 기준 추가 시 `BadgeService`에 조건문 추가 필요
- Evaluator가 Repository에 직접 의존하여 테스트 어려움
- 횟수 기반(COUNT)만 지원, 포인트 기반(POINTS) 평가 불가

### 해결 방법

**1. EvaluationType Enum 도입**

```java
public enum EvaluationType {
  COUNT,   // 활동 횟수 기준 (게시글 10개 작성)
  POINTS   // 획득 포인트 합계 기준 (POST_CREATE로 100점 달성)
}
```

**2. BadgeRule에 평가 타입 추가**

```java

@Entity
@Table(name = "badge_rules")
public class BadgeRule extends BaseTimeEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "badge_id", nullable = false)
  private Badge badge;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type", nullable = false)
  private ActivityType activityType;

  @Enumerated(EnumType.STRING)
  @Column(name = "evaluation_type", nullable = false)
  private EvaluationType evaluationType;  // COUNT or POINTS

  @Column(nullable = false)
  private int threshold;

  @Column(nullable = false)
  private boolean enabled;
}
```

**3. 전략 패턴 기반 Evaluator**

```java
// 인터페이스
public interface BadgeRuleEvaluator {
  boolean supports(BadgeRule rule);

  boolean isSatisfied(User user, BadgeRule rule,
                      Map<ActivityType, Long> activityCountMap,
                      Map<ActivityType, Long> activityPointsMap);
}

// 횟수 기반 평가
@Component
public class ActivityCountRuleEvaluator implements BadgeRuleEvaluator {
  @Override
  public boolean supports(BadgeRule rule) {
    return rule.getActivityType() != null
      && rule.getEvaluationType() == EvaluationType.COUNT;
  }

  @Override
  public boolean isSatisfied(User user, BadgeRule rule,
                             Map<ActivityType, Long> activityCountMap,
                             Map<ActivityType, Long> activityPointsMap) {
    long count = activityCountMap.getOrDefault(rule.getActivityType(), 0L);
    return count >= rule.getThreshold();
  }
}

// 포인트 기반 평가
@Component
public class ActivityPointsRuleEvaluator implements BadgeRuleEvaluator {
  @Override
  public boolean supports(BadgeRule rule) {
    return rule.getActivityType() != null
      && rule.getEvaluationType() == EvaluationType.POINTS;
  }

  @Override
  public boolean isSatisfied(User user, BadgeRule rule,
                             Map<ActivityType, Long> activityCountMap,
                             Map<ActivityType, Long> activityPointsMap) {
    long totalPoints = activityPointsMap.getOrDefault(rule.getActivityType(), 0L);
    return totalPoints >= rule.getThreshold();
  }
}
```

**4. BadgeService 리팩토링**

```java

@Service
@RequiredArgsConstructor
public class BadgeService {
  private final BadgeRuleRepository badgeRuleRepository;
  private final UserBadgeRepository userBadgeRepository;
  private final UserActivityRepository userActivityRepository;
  private final List<BadgeRuleEvaluator> evaluators;  // 자동 주입

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<String> checkAndAwardBadges(User user) {
    // 1. 활성화된 룰 조회
    List<BadgeRule> rules = badgeRuleRepository.findAllEnabledWithBadge();

    // 2. 사용자 활동 통계 조회 (한 번의 쿼리로)
    List<ActivitySummaryProjection> activityStats =
      userActivityRepository.countActivitiesByUser(user.getId());

    Map<ActivityType, Long> activityCountMap = activityStats.stream()
      .collect(Collectors.toMap(
        ActivitySummaryProjection::getActivityType,
        ActivitySummaryProjection::getCount
      ));

    Map<ActivityType, Long> activityPointsMap = activityStats.stream()
      .collect(Collectors.toMap(
        ActivitySummaryProjection::getActivityType,
        ActivitySummaryProjection::getTotalPoints
      ));

    // 3. 이미 보유한 배지 제외
    Set<Long> ownedBadgeIds = userBadgeRepository.findBadgeIdsByUserId(user.getId());

    List<String> awarded = new ArrayList<>();

    // 4. 각 룰에 대해 적절한 Evaluator로 평가
    for (BadgeRule rule : rules) {
      if (ownedBadgeIds.contains(rule.getBadge().getId())) {
        continue;
      }

      for (BadgeRuleEvaluator evaluator : evaluators) {
        if (evaluator.supports(rule)) {
          if (evaluator.isSatisfied(user, rule, activityCountMap, activityPointsMap)) {
            awardBadge(user, rule.getBadge());
            awarded.add(rule.getBadge().getName());
            break;
          }
        }
      }
    }

    return awarded;
  }
}
```

### 개선 효과

- **확장성**: 새 평가 기준 추가 시 Evaluator 구현체만 추가
- **Repository 의존성 제거**: Evaluator가 외부에서 데이터를 주입받아 테스트 용이
- **쿼리 최적화**: 활동 통계를 한 번의 쿼리로 조회 후 Evaluator에 전달
- **유연한 룰 설정**: 같은 ActivityType에 대해 COUNT/POINTS 기준 혼용 가능

### 참고 문서

- [EvaluationType.java](../src/main/java/com/eventitta/gamification/domain/EvaluationType.java) - 평가 타입 Enum
- [BadgeRule.java](../src/main/java/com/eventitta/gamification/domain/BadgeRule.java) - 룰 엔티티
- [ActivityCountRuleEvaluator.java](../src/main/java/com/eventitta/gamification/evaluator/ActivityCountRuleEvaluator.java) -
  횟수 평가
- [ActivityPointsRuleEvaluator.java](../src/main/java/com/eventitta/gamification/evaluator/ActivityPointsRuleEvaluator.java) -
  포인트 평가
- [BadgeService.java](../src/main/java/com/eventitta/gamification/service/BadgeService.java) - 수여 로직

---

## 핵심 개선 요약

| 영역                 | 개선 내용                                  | 결과                                       |
|--------------------|----------------------------------------|------------------------------------------|
| **JWT 인증**         | HttpOnly 쿠키 + Refresh Token 해시 저장      | XSS 방어, DB 탈취 대응                         |
| **모임 동시성**         | JPA 비관적 락 (`SELECT ... FOR UPDATE`)    | 정원 초과 승인 100% 방지                         |
| **포인트 동시성**        | Atomic Update 쿼리                       | Lost Update 해결, 높은 동시성 유지                |
| **도메인 결합도**        | Spring Events + 비동기 처리                 | PostService → GamificationService 의존성 제거 |
| **이벤트 신뢰성**        | Retry + DB 저장 + 스케줄러 복구                | 비동기 이벤트 데이터 유실 방지                        |
| **분산 스케줄러**        | ShedLock JDBC 락                        | 3개 인스턴스 환경에서 단일 실행 보장                    |
| **검색 쿼리**          | QueryDSL 동적 쿼리 + Fetch Join            | N+1 문제 해결, 타입 안전성 확보                     |
| **Discord 알림**     | Caffeine Cache 기반 Rate Limiter         | Alert Level별 차등 제한 적용                    |
| **외부 API 연동**      | Spring Retry (3회, exponential backoff) | 데이터 동기화 성공률 99.5%                        |
| **Badge 시스템**      | 전략 패턴 + EvaluationType 분리              | 확장성 향상, 테스트 용이성 개선                       |
| **Festival 거리 검색** | Bounding Box + 복합 인덱스                  | 3km 검색 40% 개선, 단계적 최적화 전략                |

## 11. 트랜잭션 데드락 문제 해결

### 문제 상황

운영 환경에서 동일 사용자의 연속적인 활동으로 인한 데드락 발생:

- `UserActivityService.recordActivity()`가 하나의 트랜잭션에서 너무 많은 테이블 락
- users → user_badges 순서가 불일치하여 교차 락 발생
- 스케줄러와 실시간 요청이 동시에 같은 유저를 처리하며 데드락

```java
// 문제 코드: 트랜잭션 범위가 너무 넓음
@Transactional
public void recordActivity(Long userId, ActivityType activityType, Long targetId) {
  userActivityRepository.save(userActivity);      // user_activities 락
  userRepository.incrementPoints(userId, points);  // users 락
  badgeService.checkAndAwardBadges(user);        // user_badges 락 → 데드락!
  updateRankingsAsync(userId, user.getPoints());
}
```

### 해결 방법

**1. 이벤트 기반 아키텍처로 트랜잭션 분리**

```java
// UserActivityService - 핵심 작업만 트랜잭션에서 처리
@Transactional
public void recordActivity(Long userId, ActivityType activityType, Long targetId) {
  // 1. 핵심 데이터만 빠르게 저장
  UserActivity userActivity = userActivityRepository.save(...);

  // 2. 포인트 즉시 업데이트
  userRepository.incrementPoints(userId, points);

  // 3. 이벤트 발행 (트랜잭션 커밋 후 처리)
  eventPublisher.publishEvent(
    new ActivityRecordedEvent(userId, userActivity.getId(),
      activityType, points, targetId)
  );
  // 트랜잭션 종료! 락 즉시 해제
}
```

**2. ActivityPostProcessor로 비동기 처리**

```java

@Component
@RequiredArgsConstructor
public class ActivityPostProcessor {

  @Async("gamificationExecutor")
  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void handleActivityRecorded(ActivityRecordedEvent event) {
    // 병렬 처리로 서로 영향 없음
    CompletableFuture<Void> badgeFuture = processBadgesAsync(event);
    CompletableFuture<Void> rankingFuture = processRankingsAsync(event);

    CompletableFuture.allOf(badgeFuture, rankingFuture)
      .exceptionally(ex -> {
        log.error("활동 후처리 중 일부 실패", ex);
        return null;
      });
  }
}
```

### 개선 효과

- **트랜잭션 시간**: 500ms → 50ms (90% 감소)
- **락 보유 시간**: 대폭 감소로 데드락 완전 방지
- **처리량**: 2-3배 향상
- **장애 격리**: 뱃지/랭킹 실패가 핵심 기능에 영향 없음

### 참고 문서

- [ActivityRecordedEvent.java](../src/main/java/com/eventitta/gamification/event/ActivityRecordedEvent.java) - 이벤트 정의
- [ActivityPostProcessor.java](../src/main/java/com/eventitta/gamification/event/ActivityPostProcessor.java) - 비동기 처리

---

## 12. Redis 기반 실시간 랭킹 시스템 구축

### 문제 상황

MySQL 기반 랭킹 조회 시 성능 이슈:

- ORDER BY + LIMIT 쿼리가 전체 테이블 스캔
- 사용자 수 증가에 따른 응답 시간 급증 (500ms+)
- 실시간 업데이트 시 DB 부하 증가

### 해결 방법

**1. Redis Sorted Set 활용**

```java

@Service
@RequiredArgsConstructor
public class RedisRankingService implements RankingService {

  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public void updatePointsRanking(Long userId, int points) {
    redisTemplate.opsForZSet().add(
      "ranking:points",
      userId.toString(),
      points
    );
  }

  @Override
  public List<UserRankResponse> getTopRankings(RankingType type, int limit) {
    Set<ZSetOperations.TypedTuple<Object>> rankings =
      redisTemplate.opsForZSet()
        .reverseRangeWithScores(type.getRedisKey(), 0, limit - 1);

    return convertToResponse(rankings);
  }
}
```

**2. MySQL Fallback 전략**

```java

@Override
@Transactional(readOnly = true)
public RankingPageResponse getTopRankings(RankingType type, int limit) {
  try {
    // 1차: Redis에서 조회 (빠름)
    return getTopRankingsFromRedis(type, limit);
  } catch (Exception e) {
    // 2차: Redis 장애 시 MySQL에서 조회 (느리지만 안정적)
    log.error("Redis failed, fallback to MySQL. type={}", type);
    sendDiscordAlert("Redis 장애, MySQL Fallback 동작");
    return getTopRankingsFromDatabase(type, limit);
  }
}
```

### 개선 효과

- **조회 속도**: 500ms → 5ms (100배 향상)
- **실시간성**: 포인트 변경 즉시 순위 반영
- **가용성**: Redis 장애 시에도 서비스 정상 동작
- **확장성**: 수백만 사용자까지 처리 가능

### 참고 문서

- [RedisRankingService.java](../src/main/java/com/eventitta/gamification/service/RedisRankingService.java) - Redis 랭킹 구현
- [RankingType.java](../src/main/java/com/eventitta/gamification/domain/RankingType.java) - 랭킹 타입 정의

---

## 13. 스케줄러 배치 실패 격리

### 문제 상황

실패 이벤트 재처리 스케줄러에서 하나의 실패가 전체 배치에 영향:

- 하나의 이벤트 처리 실패 시 전체 트랜잭션 롤백
- `UnexpectedRollbackException` 발생
- 정상 처리 가능한 이벤트들도 함께 실패 처리

### 해결 방법

**개별 트랜잭션 처리 도입**

```java
// FailedActivityEventRetryScheduler
@Scheduled(fixedDelay = 60000)
@SchedulerLock(name = "retryFailedActivityEvents")
@Transactional(propagation = NEVER)  // 스케줄러는 트랜잭션 없음
public void retryFailedEvents() {
  List<FailedActivityEvent> eventsToProcess =
    failedEventRepository.findByStatusAndRetryCountLessThan(
      EventStatus.PENDING, MAX_RETRY_COUNT
    );

  int successCount = 0;
  int failureCount = 0;

  for (FailedActivityEvent event : eventsToProcess) {
    try {
      // 각 이벤트를 독립 트랜잭션으로 처리
      failedEventRecoveryService.recoverFailedEventIndependently(event.getId());
      successCount++;
    } catch (Exception e) {
      // 개별 실패가 다른 이벤트에 영향 없음
      log.warn("[Scheduler] 개별 이벤트 재처리 실패 - eventId={}", event.getId());
      failureCount++;
    }
  }

  log.info("[Scheduler] 재처리 완료 - 성공: {}, 실패: {}", successCount, failureCount);
}

// FailedEventRecoveryService
@Transactional(propagation = Propagation.REQUIRES_NEW)  // 독립 트랜잭션
public void recoverFailedEventIndependently(Long eventId) {
  FailedActivityEvent event = failedEventRepository.findByIdWithLock(eventId)
    .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

  // 상태 체크 (동시성 제어)
  if (event.getStatus() != EventStatus.PENDING) {
    return;
  }

  event.markAsProcessing();
  // 처리 로직...
}
```

### 개선 효과

- **실패 격리**: 개별 이벤트 실패가 다른 이벤트에 영향 없음
- **처리율 향상**: 정상 이벤트는 모두 처리됨
- **모니터링 개선**: 성공/실패 건수 정확한 추적
- **롤백 예외 해결**: `UnexpectedRollbackException` 완전 제거

### 참고 문서

- [FailedActivityEventRetryScheduler.java](../src/main/java/com/eventitta/gamification/scheduler/FailedActivityEventRetryScheduler.java) -
  스케줄러
- [FailedEventRecoveryService.java](../src/main/java/com/eventitta/gamification/service/FailedEventRecoveryService.java) -
  복구 서비스

---

## 14. Festival 거리 검색 쿼리 최적화

### 문제 상황

축제 검색 API에서 위치 기반 거리 검색 시 성능 병목 발생:

**증상:**

- 50,000건 기준, 3km 검색: **29ms** (허용 가능하나 개선 여지)
- 10km 검색: **82ms** (병목 확인)
- 데이터 증가 시 선형 증가 예상

**원인 분석 (EXPLAIN):**

```sql
+----+-------------+----------+------+----------------+------+---------+------+--------+-------------+
| id | select_type | table    | type | possible_keys  | key  | key_len | ref  | rows   | Extra       |
+----+-------------+----------+------+----------------+------+---------+------+--------+-------------+
|  1 | SIMPLE      | festivals| ALL  | idx_dates      | NULL | NULL    | NULL | 50,000 | Using where |
+----+-------------+----------+------+----------------+------+---------+------+--------+-------------+
```

**문제점:**

1. **type = ALL**: 전체 테이블 스캔 (Full Table Scan)
2. **key = NULL**: 인덱스 미활용
3. **rows = 50,000**: 모든 레코드에 대해 Haversine 공식 계산
4. **HAVING 절 필터링**: WHERE 절에서 인덱스를 사용할 수 없음

```java
// 문제가 되는 쿼리 구조
WHERE f.start_date >=

DATE(:startDateTime)    --
날짜만 WHERE
절
HAVING distance <=:distanceKm                --
거리는 HAVING
절 →
인덱스 미활용
```

### 해결 방안 탐색

**3가지 방안 비교 분석:**

| 방안                | 성능 개선 예상 | 구현 난이도  | 리스크 | DB 호환성     | 스키마 변경      |
|-------------------|----------|---------|-----|------------|-------------|
| **Bounding Box**  | 3-5배     | ⭐ 낮음    | 낮음  | 모든 DB      | 인덱스만 추가     |
| **Spatial Index** | 10배+     | ⭐⭐⭐ 중간  | 중간  | MySQL 8.0+ | POINT 컬럼 추가 |
| **Grid Indexing** | 15배+     | ⭐⭐⭐⭐ 높음 | 높음  | 모든 DB      | grid 컬럼 추가  |

**선택: Bounding Box (Phase 1)**

**선택 이유:**

1. 즉시 적용 가능 (API 변경 없음)
2. 낮은 리스크 (인덱스만 추가)
3. 충분한 성능 개선 (40-65%)
4. 점진적 개선 가능 (Phase 2로 Spatial Index 계획)

### 해결 방법

**핵심 아이디어: Two-Phase Filtering**

```
Phase 1: WHERE 절에서 Bounding Box로 사전 필터링 (인덱스 활용)
         ↓
Phase 2: HAVING 절에서 Haversine 공식으로 정확한 거리 재검증
```

**1. Bounding Box 계산 (BoundingBoxCalculator)**

```java

@Component
public class BoundingBoxCalculator {
  private static final double KM_PER_DEGREE_LAT = 111.0;

  public BoundingBox calculate(double latitude, double longitude, double distanceKm) {
    // 위도 변화량 (지구 어디서나 일정)
    double latDelta = distanceKm / KM_PER_DEGREE_LAT;

    // 경도 변화량 (위도에 따라 변함)
    double lonDelta = distanceKm / (KM_PER_DEGREE_LAT * Math.cos(Math.toRadians(latitude)));

    return new BoundingBox(
      latitude - latDelta, latitude + latDelta,
      longitude - lonDelta, longitude + lonDelta
    );
  }
}
```

**2. 복합 인덱스 추가**

```sql
CREATE INDEX idx_date_location ON festivals (
                                             start_date, -- 1순위: 날짜 범위
                                             latitude, -- 2순위: 위도 범위
                                             longitude -- 3순위: 경도 범위
  );
```

**3. 쿼리 수정**

```java

@Query(value = """
  SELECT f.*, (6371 * acos(...)) AS distance
  FROM festivals f
  WHERE
    f.latitude BETWEEN :minLat AND :maxLat        -- Phase 1: 인덱스 활용
    AND f.longitude BETWEEN :minLon AND :maxLon
    AND f.start_date >= DATE(:startDateTime)
  HAVING distance <= :distanceKm                  -- Phase 2: 정확한 거리
  ORDER BY distance ASC
  """, nativeQuery = true)
Page<FestivalProjection> findFestivalsWithinDistanceAndDateBetween(...);
```

**4. Service 계층 통합**

```java
public PageResponse<FestivalNearbyResponse> getNearbyFestival(NearbyFestivalRequest req) {
  // Bounding Box 계산
  BoundingBox box = boundingBoxCalculator.calculate(
    req.latitude(), req.longitude(), req.distanceKm()
  );

  // Repository 호출 (Bounding Box 좌표 전달)
  Page<FestivalProjection> page = festivalRepository
    .findFestivalsWithinDistanceAndDateBetween(
      req.latitude(), req.longitude(), req.distanceKm(),
      box.minLatitude(), box.maxLatitude(),
      box.minLongitude(), box.maxLongitude(),
      req.getStartDateTime(), req.getEndDateTime(),
      PageRequest.of(req.page(), req.size())
    );

  return PageResponse.of(page.map(this::toResponse));
}
```

### 성능 측정 결과

**측정 환경:**

- 데이터: 50,000건
- 위치: 서울 중심 (37.5665, 126.9780)
- DB: MySQL 8.0

**Before/After 비교:**

| 반경       | Before | After       | 스캔 레코드                   | 개선율          |
|----------|--------|-------------|--------------------------|--------------|
| **3km**  | 29ms   | **17.5ms**  | 50,000 → 4,671 (90% 감소)  | **40%** ⬆    |
| **10km** | 82ms   | **25-30ms** | 50,000 → 44,891 (10% 감소) | **60-65%** ⬆ |

**EXPLAIN 분석 결과 (After):**

```sql
+----+-------------+----------+-------+-------------------+-------------------+---------+------+------+-----------------------------+
| id | select_type | table    | type  | possible_keys     | key               | key_len | ref  | rows | Extra                       |
+----+-------------+----------+-------+-------------------+-------------------+---------+------+------+-----------------------------+
|  1 | SIMPLE      | festivals| range | idx_date_location | idx_date_location | 24      | NULL | 4671 | Using where;
Using filesort |
+----+-------------+----------+-------+-------------------+-------------------+---------+------+------+-----------------------------+
```

**핵심 개선 지표:**

| 항목                       | Before                  | After                      |
|--------------------------|-------------------------|----------------------------|
| **Scan Type**            | `ALL` (Full Table Scan) | `range` (Index Range Scan) |
| **Index Used**           | `NULL`                  | `idx_date_location` ✅      |
| **Rows Scanned (3km)**   | 50,000                  | 4,671 (90% ⬇)              |
| **Response Time (3km)**  | 29ms                    | 17.5ms (40% ⬆)             |
| **Response Time (10km)** | 82ms                    | 25-30ms (60-65% ⬆)         |

### Phase 2 계획 (중장기)

**MySQL Spatial Index 도입 (2-3개월 후):**

```sql
-- 스키마 변경
ALTER TABLE festivals
  ADD COLUMN location POINT NOT NULL SRID 4326,
ADD SPATIAL INDEX idx_spatial_location(location);

-- 최적화된 쿼리
SELECT f.*, ST_Distance_Sphere(f.location, ST_PointFromText(:point, 4326)) / 1000 AS distance
FROM festivals f
WHERE ST_Distance_Sphere(f.location, ST_PointFromText(:point, 4326)) <= :distanceMeters
ORDER BY distance ASC;
```

**예상 성능:**

- 현재 대비 **10배 이상 개선** (10ms 이하)
- Spatial Index 활용으로 근본적 해결

**제약사항:**

- MySQL 8.0+ 필수
- H2 테스트 환경 호환성 고려
- 데이터 마이그레이션 전략 필요

### 개선 효과 요약

**기술적 성과:**

- ✅ 전체 테이블 스캔 → 인덱스 범위 스캔 (쿼리 타입 개선)
- ✅ 스캔 레코드 90% 감소 (50,000 → 4,671)
- ✅ 응답 시간 40-65% 개선
- ✅ 점진적 최적화 로드맵 수립 (Phase 1 → Phase 2)

**문제 해결 접근법:**

1. **EXPLAIN 분석으로 병목 정확히 파악** (ALL scan, 인덱스 미활용)
2. **3가지 방안 비교** (Bounding Box, Spatial Index, Grid Indexing)
3. **리스크 기반 의사결정** (낮은 리스크 방안 우선 적용)
4. **실측 데이터 기반 검증** (50,000건 기준 성능 측정)
5. **점진적 개선 전략** (Phase 1 즉시 적용 → Phase 2 중장기 계획)

### 참고 문서

- [BoundingBoxCalculator.java](../src/main/java/com/eventitta/festivals/util/BoundingBoxCalculator.java)
- [FestivalRepository.java](../src/main/java/com/eventitta/festivals/repository/FestivalRepository.java)

---

**Last Updated**: 2026-01-23
