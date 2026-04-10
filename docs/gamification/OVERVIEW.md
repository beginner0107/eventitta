# Gamification 리팩터링 개요

## 배경

이전 구조는 아래 성격이 섞여 있었습니다.

- 동기 상태값: `users.points`
- 활동 기록: `user_activities`
- 비동기 적립 경로: event listener + retry + outbox
- 파생 캐시: Redis ranking

이 모델은 기능은 동작해도 읽기와 유지보수가 어려웠습니다.

특히 아래 문제가 있었습니다.

- 코어 정합성이 비동기 체인에 분산되어 있었다.
- outbox / failed event / ranking sync가 동시에 존재해 흐름이 길었다.
- 랭킹 fallback과 stats 계산 기준이 서로 달랐다.
- 뱃지 평가가 raw activity aggregation에 의존했다.
- `post`, `comment`, `meeting` 서비스가 gamification 세부 타입을 직접 알고 있었다.

## 이번 리팩터링의 목표

- 가독성 우선
- 코어 정합성 강화
- burst TPS 상황에서도 요청 경로를 단순화
- Redis / 비동기 후처리를 projection으로 한정
- Spring Boot 서비스 계층에서 읽기 쉬운 표준 흐름으로 재정렬

## 새 설계의 핵심 원칙

### 1. 코어 상태는 동기 트랜잭션에서 끝낸다

보상 적립/회수의 기준 상태는 요청 처리 트랜잭션 안에서 끝냅니다.

즉, 포인트/활동 수의 정합성은 더 이상 스케줄러나 비동기 재처리에 의존하지 않습니다.

### 2. source of truth를 명확히 나눈다

현재 기준 source of truth는 아래 두 테이블입니다.

- `user_gamification_stats`
- `user_activity_stats`

보조 역할은 아래처럼 구분됩니다.

- `user_activities`: 현재 유효한 보상 기록 집합
- `user_badges`: 지급 완료 결과
- Redis ZSET: 랭킹 조회 projection

### 3. 도메인 서비스는 의미 있는 facade만 본다

`post`, `comment`, `meeting`, `user`는 더 이상 outbox나 retry 체인을 모릅니다.

이제 각 도메인은 아래 facade 메서드만 호출합니다.

- `onPostCreated`
- `onPostDeleted`
- `onCommentCreated`
- `onCommentDeleted`
- `onMeetingJoinApproved`
- `onMeetingJoinCancelled`
- `removeUserData`

관련 코드:

- [`GamificationFacade`](../../src/main/java/com/eventitta/gamification/service/GamificationFacade.java)
- [`DefaultGamificationFacade`](../../src/main/java/com/eventitta/gamification/service/DefaultGamificationFacade.java)

## 현재 패키지 구조

```text
com.eventitta.gamification
├── domain
│   ├── RewardActionType
│   ├── GamificationActionRecord
│   ├── UserGamificationStats
│   ├── UserActivityStats
│   ├── UserActivityStatsId
│   ├── Badge
│   ├── BadgeRule
│   ├── UserBadge
│   └── RankingType
├── service
│   ├── GamificationFacade
│   ├── DefaultGamificationFacade
│   ├── GamificationQueryService
│   ├── GamificationReconciliationService
│   ├── BadgeService
│   ├── RankingService
│   ├── RedisRankingService
│   └── NoopRankingService
├── event
│   ├── GamificationStateChangedEvent
│   ├── BadgeProjectionListener
│   └── RankingProjectionListener
├── repository
│   ├── GamificationActionRecordRepository
│   ├── UserGamificationStatsRepository
│   ├── UserActivityStatsRepository
│   ├── BadgeRuleRepository
│   └── UserBadgeRepository
└── scheduler
    └── GamificationReconciliationScheduler
```

## 데이터 모델 역할 분리

| 저장소 | 역할 | 비고 |
|--------|------|------|
| `users` | 사용자 정체성 / 프로필 / 권한 / soft delete | `points` 컬럼은 아직 남아 있지만 source of truth 아님 |
| `user_activities` | 현재 유효한 보상 기록 | 코드 개념은 `GamificationActionRecord` |
| `user_gamification_stats` | 유저 전체 포인트 / 전체 활동 수 집계 | 랭킹 fallback 기준 |
| `user_activity_stats` | 액션 타입별 집계 | 뱃지 평가 기준 |
| `user_badges` | 지급 완료 결과 | idempotent unique 보호 |
| Redis ZSET | 랭킹 projection | `POINTS`, `ACTIVITY_COUNT` |

## `ActivityType`와 `RewardActionType`의 차이

### `ActivityType`

과거 구조와 DB 호환성을 위해 남아 있는 enum입니다.

- `CREATE_POST`
- `DELETE_POST`
- `CREATE_COMMENT`
- `DELETE_COMMENT`
- `LIKE_POST`
- `LIKE_POST_CANCEL`
- `JOIN_MEETING`
- `JOIN_MEETING_CANCEL`
- `USER_LOGIN`
- 기타 legacy 항목

### `RewardActionType`

현재 코어 적립/회수 경로에서 사용하는 양의 행동 집합입니다.

- `CREATE_POST`
- `CREATE_COMMENT`
- `JOIN_MEETING`

즉, 삭제/취소는 별도 reward type이 아니라 기존 positive action의 revoke로 처리합니다.

관련 코드:

- [`ActivityType`](../../src/main/java/com/eventitta/gamification/domain/ActivityType.java)
- [`RewardActionType`](../../src/main/java/com/eventitta/gamification/domain/RewardActionType.java)

## 이전 구조 대비 변경점

| 항목 | 이전 | 현재 |
|------|------|------|
| 코어 적립 경로 | event -> listener -> retry/outbox | service -> `GamificationFacade` |
| 포인트 기준값 | `users.points` | `user_gamification_stats.total_points` |
| 활동 집계 기준값 | raw `user_activities` count/group by | `user_activity_stats`, `user_gamification_stats` |
| 배지 평가 | raw aggregation 중심 | action stats 중심 |
| 랭킹 업데이트 | 비동기 후처리 + 별도 sync | `AFTER_COMMIT` projection + reconciliation |
| 스케줄러 | 여러 개 | `GamificationReconciliationScheduler` 하나 |

## source of truth 매트릭스

| 기능 | 기준 저장소 | 비고 |
|------|-------------|------|
| 총 포인트 | `user_gamification_stats.total_points` | 코어 기준 |
| 총 활동 수 | `user_gamification_stats.total_activity_count` | 코어 기준 |
| 액션별 횟수 | `user_activity_stats.action_count` | 뱃지 기준 |
| 액션별 포인트 합 | `user_activity_stats.points_total` | 뱃지 기준 |
| 적립 대상 존재 여부 | `user_activities` unique row | 중복 적립 방지 |
| 랭킹 조회 | Redis ZSET 우선, stats fallback | projection |
| 뱃지 지급 여부 | `user_badges` | 결과 저장 |

## 도메인별 연결 방식

### Post / Comment / Meeting

도메인 서비스는 비즈니스 이벤트에 맞는 facade 메서드만 호출합니다.

- 게시글 작성 -> `onPostCreated`
- 게시글 삭제 -> `onPostDeleted`
- 댓글 작성 -> `onCommentCreated`
- 댓글 삭제 -> `onCommentDeleted`
- 모임 승인 -> `onMeetingJoinApproved`
- 모임 승인 취소 / 탈퇴 -> `onMeetingJoinCancelled`

좋아요는 더 이상 gamification 적립/회수 경로에 연결하지 않습니다.

### User

회원 탈퇴 시에는 아래 순서로 정리합니다.

1. 모임 데이터 정리
2. gamification 데이터 삭제
3. soft delete
4. Redis 랭킹 멤버 제거

## 남겨둔 호환성 요소

1차 파동에서는 아래를 의도적으로 즉시 삭제하지 않았습니다.

- `users.points` 컬럼
- 과거 migration으로 생성된 `activity_outbox`, `failed_activity_events` 테이블
- `ActivityType`의 legacy 값

이들은 런타임 코어 경로를 위해 남긴 것이 아니라, 무중단 정리와 후속 cleanup 여지를 위해 남겨둔 호환성 자산입니다.

## 다음에 볼 문서

- 실행 흐름: [RUNTIME_FLOW.md](./RUNTIME_FLOW.md)
- 운영/마이그레이션: [MIGRATION_AND_OPERATIONS.md](./MIGRATION_AND_OPERATIONS.md)
