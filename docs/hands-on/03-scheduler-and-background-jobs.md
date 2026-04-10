# 3. 스케줄러와 백그라운드 작업

이 문서는 현재 코드에 실제로 존재하는 스케줄 기반 작업만 정리합니다.

## 빠른 구분

- 스케줄 기반 작업: cron + `@Scheduled`
- 동시 실행 방지: `@SchedulerLock`
- 활성화 조건: `@ConditionalOnProperty`
- 수동 HTTP 트리거: 축제 sync admin API
- 별도 모니터링 작업: `LogMonitor`

## 현재 존재하는 작업 목록

| 작업 | 클래스 | 활성화 설정 | cron | ShedLock 이름 | 핵심 의존성 |
| --- | --- | --- | --- | --- | --- |
| 전국/서울 축제 sync | `FestivalScheduler` | `scheduler.festival-sync.enabled` | 전국: `0 0 2 1 1,4,7,10 *`, 서울: `0 0 11 * * *` | `syncNationalFestivalData`, `syncSeoulFestivalData` | `FestivalService` |
| 종료 모임 상태 반영 | `MeetingStatusScheduler` | `scheduler.meeting-status.enabled` | `0 * * * * *` | `markFinishedMeetings` | `MeetingRepository` |
| 만료 refresh token 정리 | `RefreshTokenCleanupScheduler` | `scheduler.token-cleanup.enabled` | `0 0 * * * *` | `removeExpiredRefreshTokens` | `RefreshTokenRepository` |
| stale media asset 정리 | `MediaAssetCleanupScheduler` | `scheduler.image-cleanup.enabled` | `0 0 4 * * SUN` | `cleanupStaleMediaAssets` | `MediaAssetService` |
| 게임화 projection 재정합 | `GamificationReconciliationScheduler` | `scheduler.gamification-reconciliation.enabled` | `0 0 4 * * *` | `GamificationReconciliationScheduler_rebuildRankings` | `GamificationReconciliationService` |
| 로그 모니터링 | `LogMonitor` | `monitoring.log.enabled` | 에러: `0 */1 * * * *`, 크기: `0 */10 * * * *` | 없음 | `AlertNotificationService` |

## 작업별 메모

### 1. `FestivalScheduler`

- 클래스: `eventitta-infra/src/main/java/com/eventitta/infra/festivals/scheduler/FestivalScheduler.java`
- 실행 내용:
  - 전국 축제 데이터 분기별 적재
  - 서울시 축제 데이터 일별 동기화
- 읽을 때 볼 포인트:
  - 수동 admin API와 동일한 `FestivalService`를 재사용한다.
  - 예외를 다시 던지지 않고 로그만 남긴다.

### 2. `MeetingStatusScheduler`

- 클래스: `eventitta-infra/src/main/java/com/eventitta/infra/meeting/scheduler/MeetingStatusScheduler.java`
- 실행 내용:
  - 종료 시각이 지난 모임을 `FINISHED`로 전환
- 핵심 호출:
  - `meetingRepository.updateStatusToFinished(MeetingStatus.FINISHED, now)`
- 읽을 때 볼 포인트:
  - 스케줄러가 도메인 서비스가 아니라 repository bulk update를 직접 호출한다.
  - 실행 주기가 1분이라 로컬에서도 비교적 빨리 관찰 가능하다.

### 3. `RefreshTokenCleanupScheduler`

- 클래스: `eventitta-infra/src/main/java/com/eventitta/infra/auth/scheduler/RefreshTokenCleanupScheduler.java`
- 실행 내용:
  - 만료된 refresh token row 삭제
- 핵심 호출:
  - `refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now(clock))`
- 읽을 때 볼 포인트:
  - 인증 흐름에서 저장한 `refresh_tokens`가 백그라운드에서 정리된다.
  - 로컬에서 auth API를 여러 번 돌린 뒤 테이블 변화를 보기 좋다.

### 4. `MediaAssetCleanupScheduler`

- 클래스: `eventitta-infra/src/main/java/com/eventitta/infra/media/scheduler/MediaAssetCleanupScheduler.java`
- 실행 내용:
  - stale media asset 정리
- 핵심 호출:
  - `mediaAssetService.cleanupStaleAssets()`
- 읽을 때 볼 포인트:
  - 현재는 주간 배치다.
  - 파일/스토리지 정리 로직을 읽고 싶을 때 media domain/service와 infra storage 구현을 같이 본다.

### 5. `GamificationReconciliationScheduler`

- 클래스: `eventitta-infra/src/main/java/com/eventitta/infra/gamification/scheduler/GamificationReconciliationScheduler.java`
- 실행 내용:
  - 랭킹 재구축
  - 배지 재정합
- 핵심 호출:
  - `gamificationReconciliationService.rebuildRankings()`
  - `gamificationReconciliationService.reconcileBadges()`
- 읽을 때 볼 포인트:
  - `@Profile("!test")`라 테스트 프로파일에서는 올라오지 않는다.
  - Redis projection과 MySQL 집계를 다시 맞추는 관점에서 읽으면 된다.

### 6. `LogMonitor`

- 클래스: `eventitta-infra/src/main/java/com/eventitta/infra/common/monitoring/LogMonitor.java`
- 활성화 설정:
  - `monitoring.log.enabled=true`
- 실행 내용:
  - 최근 N분간 ERROR 로그 수 감시
  - 로그 디렉터리 용량 감시
- 읽을 때 볼 포인트:
  - `scheduler.*` 체계에 속하지 않는다.
  - Discord/Webhook 알림이 아니라도 로컬에서 로그 모니터링 로직 자체는 따라갈 수 있다.

## 수동 HTTP와 스케줄 작업을 분리해서 보기

### 수동 HTTP로 실행 가능한 것

- `POST /api/v1/admin/festivals/sync/national`
- `POST /api/v1/admin/festivals/sync/seoul`

### 로그/DB/테스트로 확인하는 것이 나은 것

- `MeetingStatusScheduler`
- `RefreshTokenCleanupScheduler`
- `MediaAssetCleanupScheduler`
- `GamificationReconciliationScheduler`
- `LogMonitor`

이 작업들은 별도 디버그 엔드포인트가 없으므로, 로컬에서는 다음 세 가지로 확인하는 편이 안전합니다.

1. 로그 확인
2. 관련 테이블/Redis 키 조회
3. 기존 테스트 확인

## 로컬에서 안전하게 확인하는 방법

### 1. 로그로 보기

```bash
tail -f ./logs/eventitta.log | rg 'Scheduler|GamificationReconciliation|LogMonitor|지역 캐시'
```

### 2. ShedLock row 확인

```bash
MYSQL_PWD="$MYSQL_PASSWORD" mysql \
  -h 127.0.0.1 -P "${DB_PORT:-3306}" -u eventittaUser eventitta \
  -e "select name, lock_until, locked_at, locked_by from shedlock order by name;"
```

### 3. Health 확인

```bash
curl http://localhost:8080/actuator/health
```

### 4. 관련 테스트로 lock 이름과 동시 실행 방지 확인

핵심 테스트:

- `eventitta-app/src/test/java/com/eventitta/scheduler/ShedLockIntegrationTest.java`

이 테스트에서 실제로 확인하는 것:

- `syncNationalFestivalData`
- `syncSeoulFestivalData`
- `markFinishedMeetings`
- `removeExpiredRefreshTokens`

즉, 코드만 읽지 말고 테스트에서 어떤 락 이름을 기대하는지도 같이 보아야 합니다.

## 운영 설정을 읽는 기준

현재 활성화 예시는 [../SCHEDULER_CONFIG_EXAMPLE.yml](../SCHEDULER_CONFIG_EXAMPLE.yml)에 정리했습니다.

읽을 때 기준:

- `scheduler.*.enabled=false`면 해당 Bean 자체가 올라오지 않는다.
- `matchIfMissing=true`라 설정이 없으면 기본적으로 활성화된다.
- `LogMonitor`는 `monitoring.log.enabled`를 별도로 본다.

## 다음 단계

스케줄러만 읽고 끝내지 말고, [04-sql-cache-redis-observability.md](./04-sql-cache-redis-observability.md)에서 실제 SQL, Redis 키, 캐시를 같이 관찰해 보는 것이 좋습니다.
