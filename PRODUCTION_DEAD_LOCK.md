# 🚨 시스템 장애 분석 보고서: DB 락 경합 및 데드락

## 1. 장애 개요

| 항목         | 내용                                             |
|------------|------------------------------------------------|
| **장애 일시**  | 2025-12-31 13:08:01 ~ 진행 중                     |
| **영향 범위**  | 가미피케이션 서비스 (활동 기록, 포인트 지급, 뱃지 부여)              |
| **핵심 원인**  | 특정 사용자(`userId=2`)의 연속적인 활동으로 인한 DB 락 경합 및 데드락 |
| **영향 테이블** | `users`, `user_badges`, `user_activities`      |

---

## 2. 문제 상황 상세

### 2.1 현재 코드 구조의 문제점

`UserActivityService.recordActivity()` 메서드가 **하나의 트랜잭션 안에서 너무 많은 작업**을 수행합니다.

```java

@Transactional  // ← 모든 작업이 하나의 트랜잭션
public void recordActivity(Long userId, ActivityType activityType, Long targetId) {

  // 1단계: 활동 저장 (user_activities 테이블 락)
  userActivityRepository.save(userActivity);

  // 2단계: 포인트 업데이트 (users 테이블 락) 🔒
  userRepository.incrementPoints(userId, points);

  // 3단계: 뱃지 체크 (user_badges 테이블 락) 🔒
  badgeService.checkAndAwardBadges(user);  // REQUIRES_NEW이지만 부모가 락을 쥐고 있음

  // 4단계: 랭킹 업데이트
  updateRankingsAsync(userId, user.getPoints());

  // ← 여기까지 와야 모든 락이 해제됨
}
```

### 2.2 데드락 발생 메커니즘

```
시간 ────────────────────────────────────────────────────────────►

[스레드 A - 실시간 요청]
     │
     ├─ users 테이블 락 획득 🔒
     │
     ├─ (포인트 업데이트 중...)
     │
     ├─ user_badges 테이블 락 필요... ⏳ 대기
     │
     ▼
     
[스레드 B - 스케줄러]
     │
     ├─ user_badges 테이블 락 획득 🔒
     │
     ├─ (뱃지 확인 중...)
     │
     ├─ users 테이블 락 필요... ⏳ 대기
     │
     ▼

💀 DEADLOCK: 서로가 서로의 락을 기다리며 무한 대기
```

### 2.3 장애 타임라인

| 시간    | 단계      | 상황                             | 오류                                   |
|-------|---------|--------------------------------|--------------------------------------|
| 13:07 | 정상      | 게시글 생성 성공                      | -                                    |
| 13:08 | 락 대기 시작 | 댓글 생성 시 뱃지 지급 중 50초 대기         | `Lock wait timeout exceeded`         |
| 13:10 | 실패 기록   | Slack 알림 발송, 실패 이벤트 저장         | `PessimisticLockingFailureException` |
| 13:11 | 악순환 시작  | 스케줄러 재시도 + 실시간 요청 동시 진행        | 락 경합 심화                              |
| 13:19 | 데드락 발생  | `users` ↔ `user_badges` 교차 락   | `Deadlock found`                     |
| 13:19 | 트랜잭션 오염 | 롤백 마킹된 트랜잭션에서 커밋 시도            | `UnexpectedRollbackException`        |
| 13:20 | 장애 지속   | 실패 건수 증가 (1건 → 3건), 스케줄러 무한 루프 | -                                    |

### 2.4 근본 원인 분석

| 원인                   | 설명                                          | 코드 위치                                  |
|----------------------|---------------------------------------------|----------------------------------------|
| **트랜잭션 범위 과다**       | 활동 저장 + 포인트 + 뱃지를 하나의 트랜잭션에서 처리             | `UserActivityService.recordActivity()` |
| **REQUIRES_NEW 부작용** | 새 트랜잭션을 열지만, 부모가 `users` 락을 쥐고 있어 대기 발생     | `BadgeService.checkAndAwardBadges()`   |
| **락 순서 불일치**         | 스레드마다 `users` → `user_badges` 또는 그 반대 순서로 락 | 전체 구조                                  |
| **동일 유저 동시 처리**      | 같은 userId에 대해 여러 스레드가 동시 접근                 | 스케줄러 + 실시간                             |
| **스케줄러 트랜잭션 전파 문제**  | 개별 이벤트 실패 시 전체 배치 롤백                        | `FailedActivityEventRetryScheduler`    |

---

## 3. 해결 방안 선택지

### 방안 A: 트랜잭션 분리 (권장)

**개념**: 핵심 작업(활동 저장, 포인트)과 부가 작업(뱃지, 랭킹)을 분리

**장점**:

- 락 보유 시간 최소화
- 부가 작업 실패 시에도 핵심 데이터 보존
- 근본적인 구조 개선

**단점**:

- 코드 수정 범위가 넓음
- 테스트 케이스 재작성 필요

**구현 방향**:

```java

@Transactional
public void recordActivity(...) {
  // 1단계: 핵심 데이터만 빠르게 저장 (락 즉시 해제)
  userActivityRepository.save(userActivity);
  userRepository.incrementPoints(userId, points);
}

// 트랜잭션 커밋 후 별도 처리
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void processPostActivity(...) {
  // 2단계: 뱃지, 랭킹은 별도 트랜잭션
  badgeService.checkAndAwardBadges(userId);
  rankingService.updateRankings(userId);
}
```

**예상 효과**: 락 보유 시간 80% 감소, 데드락 원천 차단

---

### 방안 B: 분산 락 적용 (Redis)

**개념**: 같은 userId에 대해 한 번에 하나의 스레드만 처리

**장점**:

- 기존 코드 구조 최소 변경
- 동시성 문제 완전 해결
- 다른 서비스에도 재사용 가능

**단점**:

- Redis 의존성 추가
- 락 획득 대기 시간 발생
- 단일 장애점(SPOF) 우려

**구현 방향**:

```java

@Transactional(propagation = Propagation.REQUIRES_NEW)
public List<String> checkAndAwardBadges(Long userId) {
  RLock lock = redissonClient.getLock("badge:lock:user:" + userId);

  try {
    if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
      return Collections.emptyList(); // 다음 기회에 처리
    }
    // 뱃지 체크 로직
  } finally {
    lock.unlock();
  }
}
```

**예상 효과**: 동일 유저 동시 처리 방지, 데드락 90% 감소

---

### 방안 C: 스케줄러 개별 트랜잭션 처리

**개념**: 배치 단위가 아닌 이벤트 단위로 독립 트랜잭션 처리

**장점**:

- 구현 간단
- 개별 실패가 다른 이벤트에 영향 없음
- 즉시 적용 가능

**단점**:

- 근본적인 데드락 원인은 해결되지 않음
- 트랜잭션 오버헤드 증가

**구현 방향**:

```java

@Scheduled(fixedDelay = 60000)
public void retryFailedEvents() {
  List<FailedActivityEvent> events = repository.findPendingEvents();

  for (FailedActivityEvent event : events) {
    try {
      // 각 이벤트를 개별 트랜잭션으로 처리
      recoveryService.recoverFailedEventIndependently(event.getId());
    } catch (Exception e) {
      // 개별 실패는 다음 이벤트 처리에 영향 없음
      log.warn("이벤트 처리 실패: {}", event.getId());
    }
  }
}
```

**예상 효과**: `UnexpectedRollbackException` 해결, 스케줄러 안정화

---

### 방안 D: 락 순서 통일

**개념**: 모든 코드에서 테이블 락 순서를 동일하게 유지

**장점**:

- 데드락 원천 차단
- 코드 수정 최소화

**단점**:

- 모든 관련 코드 검토 필요
- 실수 가능성 높음
- 유지보수 어려움

**구현 방향**:

```
모든 트랜잭션에서 락 순서 통일:
1. users (항상 첫 번째)
2. user_activities (두 번째)
3. user_badges (세 번째)
```

**예상 효과**: 교차 락으로 인한 데드락 방지

---

### 방안 E: 비동기 큐 기반 처리 (장기)

**개념**: 활동 기록을 메시지 큐에 넣고 순차적으로 처리

**장점**:

- 완전한 비동기 처리
- 트래픽 급증에도 안정적
- 확장성 우수

**단점**:

- 아키텍처 변경 필요
- 메시지 큐 인프라 추가 (Kafka, RabbitMQ)
- 구현 복잡도 높음
- 실시간성 저하

**구현 방향**:

```
[사용자 활동] → [Kafka Queue] → [Consumer] → [DB 저장]
                                    ↓
                              순차적 처리로
                              동시성 문제 해결
```

**예상 효과**: 동시성 문제 완전 해결, 시스템 안정성 극대화

---

## 4. 방안 비교 매트릭스

| 방안                  | 구현 난이도 | 효과    | 적용 시간 | 리스크 | 권장 순위    |
|---------------------|--------|-------|-------|-----|----------|
| **A. 트랜잭션 분리**      | 중      | 높음    | 2-3일  | 낮음  | ⭐ 1순위    |
| **B. 분산 락 (Redis)** | 중      | 높음    | 1-2일  | 중간  | ⭐ 2순위    |
| **C. 스케줄러 개선**      | 낮음     | 중간    | 즉시    | 낮음  | 🚨 긴급 적용 |
| **D. 락 순서 통일**      | 낮음     | 중간    | 1일    | 중간  | 3순위      |
| **E. 비동기 큐**        | 높음     | 매우 높음 | 1-2주  | 중간  | 장기 과제    |

---

## 5. 권장 적용 순서

### 🚨 긴급 (즉시)

1. **스케줄러 일시 중단**: 현재 장애를 가중시키고 있음
2. **DB 락 강제 해제**: `SHOW ENGINE INNODB STATUS` → `KILL [session_id]`
3. **방안 C 적용**: 스케줄러 개별 트랜잭션 처리

### 📅 단기 (1-3일)

4. **방안 B 적용**: BadgeService에 분산 락 적용
5. **방안 D 적용**: 락 순서 코드 리뷰 및 통일

### 📅 중기 (1-2주)

6. **방안 A 적용**: UserActivityService 트랜잭션 구조 전면 개선

### 📅 장기 (1개월+)

7. **방안 E 검토**: 트래픽 증가 대비 아키텍처 개선

---

## 6. 긴급 조치 명령어

### DB 락 상태 확인

```sql
-- 현재 락 대기 상태 확인
SHOW
ENGINE INNODB STATUS;

-- 락을 잡고 있는 트랜잭션 확인
SELECT *
FROM information_schema.INNODB_LOCKS;
SELECT *
FROM information_schema.INNODB_LOCK_WAITS;

-- 장시간 실행 중인 쿼리 확인
SELECT *
FROM information_schema.PROCESSLIST
WHERE TIME > 30;
```

### 락 강제 해제

```sql
-- 특정 세션 강제 종료 (ID는 위 쿼리에서 확인)
KILL
[session_id];
```

### 스케줄러 비활성화 (application.yml)

```yaml
spring:
  task:
    scheduling:
      enabled: false
```

---

## 7. 모니터링 강화 권장사항

| 항목       | 임계치     | 알림       |
|----------|---------|----------|
| 락 대기 시간  | > 10초   | Slack 경고 |
| 데드락 발생   | 1회 이상   | Slack 긴급 |
| 실패 이벤트 수 | > 10건/분 | Slack 경고 |
| 트랜잭션 롤백률 | > 5%    | Slack 경고 |

---

## 8. 결론

현재 장애의 **근본 원인**은 `UserActivityService.recordActivity()` 메서드가 너무 많은 작업을 하나의 트랜잭션에서 처리하면서 **락 보유 시간이 길어지고**, 스케줄러와 실시간
요청이 **서로 다른 순서로 락을 획득**하려다 데드락이 발생한 것입니다.

**즉시 방안 C를 적용**하여 스케줄러로 인한 장애 가중을 막고, **단기적으로 방안 A+B를 조합**하여 근본적인 구조 개선을 진행하는 것을 권장합니다.
