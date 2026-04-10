# Gamification 마이그레이션 및 운영 메모

## 목적

이 문서는 리팩터링 이후 운영 관점에서 알아야 할 내용을 정리합니다.

포함 범위:

- DB 변경 사항
- 현재 롤아웃 상태
- 운영 설정
- 복구 전략
- 남아 있는 cleanup 항목

## 1. DB 변경 사항

이번 리팩터링의 핵심 migration은 아래 파일입니다.

- [`V16__Add_gamification_stats_tables.sql`](../../src/main/resources/db/migration/V16__Add_gamification_stats_tables.sql)

추가된 테이블:

- `user_gamification_stats`
- `user_activity_stats`

## `user_gamification_stats`

| 컬럼 | 의미 |
|------|------|
| `user_id` | PK |
| `total_points` | 전체 포인트 |
| `total_activity_count` | 전체 활동 수 |
| `updated_at` | 마지막 갱신 시각 |

## `user_activity_stats`

| 컬럼 | 의미 |
|------|------|
| `user_id` | PK 일부 |
| `action_type` | PK 일부 |
| `action_count` | 액션별 횟수 |
| `points_total` | 액션별 포인트 누적 |
| `updated_at` | 마지막 갱신 시각 |

## backfill 방식

초기 적재는 아래 기준을 사용합니다.

- `user_gamification_stats.total_points` <- `users.points`
- `user_gamification_stats.total_activity_count` <- `user_activities` 집계
- `user_activity_stats` <- `user_activities` by `user_id`, `activity_type`

## 2. 현재 롤아웃 상태

현재 코드 기준 상태는 아래와 같습니다.

### 이미 전환된 것

- 적립/회수 코어 로직
- domain service -> `GamificationFacade` 호출
- 배지 평가 기준
- 랭킹 fallback 기준
- Redis ranking rebuild 기준
- 회원 탈퇴 시 gamification 정리

### 아직 남겨둔 것

- `users.points` 컬럼
- `ActivityType`의 legacy 타입들
- 과거 migration으로 생성된 `activity_outbox`
- 과거 migration으로 생성된 `failed_activity_events`

## 3. 운영 설정

### reconciliation 스케줄러

현재 유지하는 게임화 스케줄러는 하나입니다.

- `scheduler.gamification-reconciliation.enabled`

테스트 프로파일에서는 기본적으로 꺼져 있습니다.

관련 파일:

- [`application-test.yml`](../../src/main/resources/application-test.yml)
- [`SchedulingConfig`](../../src/main/java/com/eventitta/common/config/scheduling/SchedulingConfig.java)

### startup warm-up

애플리케이션 시작 시 Redis ranking projection이 비어 있으면 1회 full rebuild를 수행합니다.

관련 코드:

- [`GamificationReconciliationService.warmUpRankingsOnStartup`](../../src/main/java/com/eventitta/gamification/service/GamificationReconciliationService.java)

## 4. 장애 및 복구 전략

### 코어 정합성

코어 정합성은 아래 저장소만으로 보장됩니다.

- `user_activities`
- `user_gamification_stats`
- `user_activity_stats`

즉, Redis 장애나 projection listener 실패가 있어도 포인트/활동 기준값은 DB에 남습니다.

### 랭킹 장애

Redis projection이 실패하거나 비어 있으면:

1. 조회 시 DB fallback
2. 앱 시작 시 warm-up
3. 일일 reconciliation rebuild

### 배지 projection 실패

배지 후처리가 실패해도 요청 자체는 rollback 하지 않습니다.

복구는:

1. 이후 동일 action 발생 시 재평가
2. 일일 `reconcileBadges`

## 5. 운영 중 확인해야 할 지표

### 정합성 관점

- 특정 유저 포인트가 `users.points`와 `user_gamification_stats.total_points`에서 다른지
- Redis ranking member 수가 stats 양수 유저 수와 크게 어긋나는지
- `user_activity_stats` row 수가 비정상적으로 줄거나 늘었는지

### 성능 관점

- reward endpoint 평균 응답 시간
- `user_activities` unique insert 충돌률
- stats update lock wait
- Redis ZSET update 실패 로그 빈도

## 6. 테스트 커버리지

이번 구조에 맞춰 아래 테스트를 추가/정리했습니다.

### 코어 통합 테스트

- [`DefaultGamificationFacadeIntegrationTest`](../../src/test/java/com/eventitta/gamification/service/DefaultGamificationFacadeIntegrationTest.java)
- [`GamificationFacadeConcurrencyTest`](../../src/test/java/com/eventitta/gamification/service/GamificationFacadeConcurrencyTest.java)

### projection / fallback 테스트

- [`GamificationReconciliationServiceTest`](../../src/test/java/com/eventitta/gamification/service/GamificationReconciliationServiceTest.java)
- [`RankingServiceTest`](../../src/test/java/com/eventitta/gamification/service/RankingServiceTest.java)

### 도메인 서비스 회귀

- [`PostServiceTest`](../../src/test/java/com/eventitta/post/service/PostServiceTest.java)
- [`CommentServiceTest`](../../src/test/java/com/eventitta/comment/service/CommentServiceTest.java)
- [`MeetingServiceTest`](../../src/test/java/com/eventitta/meeting/service/MeetingServiceTest.java)
- [`UserServiceIntegrationTest`](../../src/test/java/com/eventitta/user/service/UserServiceIntegrationTest.java)

## 7. 후속 cleanup 항목

이번 파동에서 일부러 미룬 정리입니다.

1. `users.points` 컬럼 제거
2. `UserRepository.incrementPoints`, `decrementPoints` 정리
3. `ActivityType`의 legacy cancel/delete 값 제거 여부 결정
4. 과거 outbox / failed event 테이블의 실제 제거 migration
5. README / ARCHITECTURE 전체 문서에서 옛 outbox 설명 완전 제거

## 8. 운영자 관점 최종 요약

- 현재 코어 상태는 DB stats 기준으로 읽어야 합니다.
- Redis는 projection이며, 비어도 복구 가능합니다.
- 스케줄러는 self-healing 용도입니다.
- 기존 outbox/retry 라인은 더 이상 런타임 경로가 아닙니다.
