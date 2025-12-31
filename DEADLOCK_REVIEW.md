# 코드 리뷰 결과 요약

## 🎯 핵심 문제 (데드락 해결과 직결)

### 1. 예외 처리 누락 - 에러가 발생해도 모름

**현재 코드**

```java
public void handleActivityRecorded(ActivityRecordedEvent event) {
  CompletableFuture<Void> badgeFuture = processBadgesAsync(event);
  CompletableFuture<Void> rankingFuture = processRankingsAsync(event);
  // 결과 확인 안 함
}
```

**문제점**

- 뱃지/랭킹 처리 실패 시 로그 없음
- 장애 발생해도 원인 추적 불가

**수정안**

```java
CompletableFuture<Void> badgeFuture = processBadgesAsync(event)
  .exceptionally(e -> {
    log.error("뱃지 처리 실패 - userId={}", event.userId(), e);
    return null;
  });
```

---

### 2. 이벤트 발행 시점 확인 필요

**확인 필요**

```java
// UserActivityService.java
eventPublisher.publishEvent(new ActivityRecordedEvent(...));

// ActivityPostProcessor.java
@TransactionalEventListener(phase = AFTER_COMMIT)  // ← 이거 있는지 확인!
public void handleActivityRecorded(ActivityRecordedEvent event) { ...}
```

**문제점**

- `AFTER_COMMIT` 없으면 트랜잭션 **내부**에서 실행됨
- 트랜잭션 분리 효과 없음 → 데드락 해결 안 됨

**확인사항**

- [ ] `ActivityPostProcessor`에 `@TransactionalEventListener(phase = AFTER_COMMIT)` 있는지 확인

---

### 3. 실패 이벤트 상태 관리 미흡

**현재 코드**

```java
public void recoverFailedEventIndependently(Long eventId) {
  FailedActivityEvent event = failedEventRepository.findById(eventId)...;
  recoverFailedEventInternal(event);  // 상태 변경 로직 없음
}
```

**문제점**

- `PENDING` → `PROCESSING` → `SUCCESS/FAILED` 상태 전이 없음
- 같은 이벤트를 여러 스레드가 동시에 처리할 수 있음

**수정안**

```java
public void recoverFailedEventIndependently(Long eventId) {
  FailedActivityEvent event = failedEventRepository.findById(eventId)...;

  if (event.getStatus() != EventStatus.PENDING) {
    return;  // 이미 처리 중이면 스킵
  }

  event.markAsProcessing();  // 상태 변경
  failedEventRepository.saveAndFlush(event);

  try {
    recoverFailedEventInternal(event);
    event.markAsSuccess();
  } catch (Exception e) {
    event.incrementRetryCount();
    event.markAsPending();  // 재시도 대기
    throw e;
  }
}
```

---

## 📌 별개 문제 (데드락과 무관, 하지만 수정 권장)

### 1. 뱃지 중복 지급 가능성

**현재 흐름**

```
스레드 A: "뱃지 있나?" → "없네" → 저장!
스레드 B: "뱃지 있나?" → "없네" → 저장!  ← 중복
```

**해결책: 이중 방어**

```sql
-- DB 제약조건 (최후의 방어선)
ALTER TABLE user_badges
  ADD CONSTRAINT uk_user_badge UNIQUE (user_id, badge_id);
```

```java
// 애플리케이션 체크 (1차) + 예외 처리 (2차)
if(userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)){
  return;
  }

  try{
  userBadgeRepository.

save(new UserBadge(user, badge));
  }catch(
DataIntegrityViolationException e){
  log.

debug("동시 저장 시도 - 무시");
}
```

---

### 2. 스레드 풀 설정 검토

**현재**

```java
executor.setCorePoolSize(3);
executor.

setMaxPoolSize(8);
executor.

setQueueCapacity(200);
executor.

setRejectedExecutionHandler(new CallerRunsPolicy());
```

**검토 필요**

- `CallerRunsPolicy`: 큐 포화 시 **메인 스레드에서 실행** → API 응답 지연 가능
- `QueueCapacity(200)`: 200개 쌓이면 처리 지연 심각

**권장 수정**

```java
executor.setQueueCapacity(100);  // 적정 수준으로
executor.

setRejectedExecutionHandler((r, e) ->{
  log.

error("작업 거부됨 - 큐 포화");
// 메트릭 수집 또는 알림
});
```

---

### 3. 모니터링 부재

**현재**: 성공/실패 메트릭 수집 없음

**권장 추가**

```java
// Micrometer 메트릭
meterRegistry.counter("gamification.badge.success").

increment();
meterRegistry.

counter("gamification.badge.failure").

increment();
meterRegistry.

timer("gamification.process.duration").

record(duration);
```

---

## ✅ 체크리스트

### 핵심 (필수)

- [ ] `ActivityPostProcessor` 예외 처리 추가
- [ ] `@TransactionalEventListener(phase = AFTER_COMMIT)` 확인
- [ ] 실패 이벤트 상태 전이 로직 추가

### 권장 (선택)

- [ ] `user_badges` 테이블 유니크 제약조건 추가
- [ ] 스레드 풀 설정 검토
- [ ] 모니터링 메트릭 추가

---

## 📊 우선순위 요약

| 순위 | 문제              | 영향도 | 난이도 |
|----|-----------------|-----|-----|
| 1  | 예외 처리 누락        | 높음  | 낮음  |
| 2  | AFTER_COMMIT 확인 | 높음  | 낮음  |
| 3  | 상태 전이 로직        | 중간  | 중간  |
| 4  | 뱃지 중복 방지        | 중간  | 낮음  |
| 5  | 스레드 풀 설정        | 낮음  | 낮음  |
| 6  | 모니터링 추가         | 낮음  | 중간  |
