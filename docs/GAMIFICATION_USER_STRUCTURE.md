# User / Gamification 문서 가이드

## 목적

이번 리팩터링으로 `user` / `gamification` 구조가 크게 바뀌었습니다.

기존 문서는 `activity_outbox`, `failed_activity_events`, 다중 스케줄러, `users.points` 중심 구조를 기준으로 적혀 있었고, 현재 코드와 맞지 않는 설명이 섞여 있었습니다.

이 문서는 상세 설명을 위한 진입점입니다.

## 현재 구조 한 줄 요약

- 게임화 코어 적립/회수는 `GamificationFacade`에서 동기 트랜잭션으로 처리합니다.
- 상태의 기준값은 `user_gamification_stats`, `user_activity_stats`입니다.
- `user_activities`는 현재 유효한 보상 기록 집합을 담는 `GamificationActionRecord` 개념으로 사용합니다.
- Redis 랭킹과 배지 지급은 `AFTER_COMMIT` projection으로 처리합니다.
- 스케줄러는 `GamificationReconciliationScheduler` 하나만 유지합니다.

## 문서 구성

### 1. 구조 개요

- [docs/gamification/OVERVIEW.md](./gamification/OVERVIEW.md)
- 무엇이 바뀌었는지
- 왜 이렇게 바꿨는지
- 현재 source of truth가 무엇인지
- 핵심 클래스와 책임이 어떻게 나뉘는지

### 2. 런타임 흐름

- [docs/gamification/RUNTIME_FLOW.md](./gamification/RUNTIME_FLOW.md)
- 게시글/댓글/좋아요/모임 승인 시 적립 흐름
- 취소/삭제 시 회수 흐름
- 회원 탈퇴 정리 흐름
- 랭킹 조회와 projection 흐름

### 3. 마이그레이션 및 운영

- [docs/gamification/MIGRATION_AND_OPERATIONS.md](./gamification/MIGRATION_AND_OPERATIONS.md)
- DB 마이그레이션과 현재 롤아웃 상태
- 남아 있는 호환성 자산
- 운영 설정과 복구 전략
- 후속 cleanup 항목

## 빠른 시작

코드 진입점만 먼저 보려면 아래 순서로 읽으면 됩니다.

1. [`DefaultGamificationFacade`](../src/main/java/com/eventitta/gamification/service/DefaultGamificationFacade.java)
2. [`GamificationStateChangedEvent`](../src/main/java/com/eventitta/gamification/event/GamificationStateChangedEvent.java)
3. [`BadgeProjectionListener`](../src/main/java/com/eventitta/gamification/event/BadgeProjectionListener.java)
4. [`RankingProjectionListener`](../src/main/java/com/eventitta/gamification/event/RankingProjectionListener.java)
5. [`GamificationReconciliationService`](../src/main/java/com/eventitta/gamification/service/GamificationReconciliationService.java)

## 현재 기준으로 더 이상 사용하지 않는 런타임 경로

아래 체인은 코드에서 제거되었습니다.

- `ActivityEventPublisher`
- `ActivityOutboxWriter`
- `OutboxRelayService`
- `UserActivityEventListener`
- `FailedEventRecoveryService`
- `RankingSyncService`
- `FailedActivityEventRetryScheduler`
- `OutboxRelayScheduler`
- `RankingScheduler`

스키마에는 과거 migration 흔적이 남아 있을 수 있지만, 현재 애플리케이션 런타임 경로는 위 체계를 사용하지 않습니다.
