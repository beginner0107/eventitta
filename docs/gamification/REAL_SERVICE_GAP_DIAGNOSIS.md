# Eventitta 게이미피케이션 실서비스 허점 진단서

## 기준

- 비교 기준은 외부 사례가 아니라 지역 커뮤니티/모임 서비스가 일반적으로 요구하는 운영 원칙입니다.
- 범위는 뱃지 수여, 포인트 적립/회수, 랭킹 노출/정합성, 악용 방지, 운영 복구 가능성입니다.
- 확인 결과는 현재 코드와 테스트를 기준으로 정리했습니다.

## 현재 코드가 이미 잘하는 부분

- 핵심 적립/회수 정합성은 `GamificationInternalFacade` 한 경로로 모여 있고, 중복 적립은 `user_activities` unique 제약으로 막습니다.
  근거: `eventitta-domain/src/main/java/com/eventitta/domain/gamification/service/DefaultGamificationFacade.java:86-119`, `eventitta-domain/src/main/java/com/eventitta/domain/gamification/domain/GamificationActionRecord.java:15-18`
- 총점/활동 수는 `user_gamification_stats`, 액션별 집계는 `user_activity_stats`로 분리되어 있어 raw aggregation 의존은 줄었습니다.
  근거: `docs/gamification/OVERVIEW.md:111-118`
- Redis 랭킹 장애 시 DB fallback 경로가 있고, projection 복구 스케줄도 존재합니다.
  근거: `eventitta-infra/src/main/java/com/eventitta/infra/gamification/service/RedisRankingService.java:52-56`, `eventitta-domain/src/main/java/com/eventitta/domain/gamification/service/GamificationReconciliationService.java:34-69`
- 선택 실행한 테스트는 통과했습니다.
  실행 명령: `./gradlew :eventitta-app:test --tests 'com.eventitta.api.gamification.controller.RankingControllerTest' --tests 'com.eventitta.api.user.controller.UserControllerTest' --tests 'com.eventitta.domain.gamification.service.DefaultGamificationFacadeIntegrationTest' --tests 'com.eventitta.domain.gamification.service.GamificationFacadeConcurrencyTest' :eventitta-infra:test --tests 'com.eventitta.infra.gamification.service.RankingServiceTest' :eventitta-domain:test --tests 'com.eventitta.domain.gamification.service.BadgeServiceTest'`

## P1

### 1. 모임 포인트가 실제 참석이 아니라 승인 시점에 지급된다

실서비스 차이:
이벤트/모임 서비스에서 보상은 보통 출석 확정, 체크인, 종료 후 후기 작성 같은 실제 참여 신호에 묶입니다. 지금 구조는 리더 승인만 받으면 즉시 포인트가 쌓입니다.

코드 근거:
`approveParticipant`가 `participant.approve()` 직후 바로 `gamificationFacade.onMeetingJoinApproved(...)`를 호출합니다. 참가 상태 enum도 `PENDING`, `APPROVED`, `REJECTED`뿐이라 출석/노쇼 상태가 없습니다.

- `eventitta-domain/src/main/java/com/eventitta/domain/meeting/service/MeetingService.java:183-207`
- `eventitta-domain/src/main/java/com/eventitta/domain/meeting/domain/ParticipantStatus.java:3-6`

영향:
승인만 받고 실제 참석하지 않는 유저가 포인트를 농사할 수 있습니다. 취소한 경우에는 회수되지만, 그냥 노쇼인 경우 회수 경로가 없습니다. 이벤트 서비스 기준으로는 가장 큰 허점입니다.

### 2. 해결됨: 좋아요는 더 이상 포인트 적립 경로가 아니다

실서비스 차이:
현재 구조에서는 좋아요 자체를 gamification 대상에서 제거했기 때문에 self-like 여부와 무관하게 포인트 적립은 발생하지 않습니다.

코드 근거:
`PostService.likePost(...)`는 `PostLike` 저장만 수행하고, 더 이상 `gamificationFacade`를 호출하지 않습니다.

- `eventitta-domain/src/main/java/com/eventitta/domain/post/service/PostService.java`
- `eventitta-domain/src/main/java/com/eventitta/domain/post/domain/PostLike.java:17-28`

영향:
좋아요를 반복해도 포인트, 배지, 랭킹에는 영향을 주지 않습니다. self-like는 여전히 제품 정책 문제일 수 있지만, gamification 농사 경로는 아닙니다.

### 3. 랭킹의 Redis 경로와 DB fallback 경로가 같은 규칙을 보장하지 않는다

실서비스 차이:
실서비스 랭킹은 실시간 경로와 fallback 경로가 같은 정렬 규칙과 같은 참여자 기준을 가져야 합니다. 지금은 문서상 규칙과 Redis 구현이 맞지 않습니다.

코드 근거:
문서는 fallback tie-break를 `userId ASC`로 명시하지만, Redis 조회는 `reverseRangeWithScores`만 사용하고 별도 numeric tie-break를 적용하지 않습니다. 반면 DB 쿼리는 `order by ... userId asc`를 강제합니다.

- `docs/gamification/RUNTIME_FLOW.md:147-149`
- `eventitta-infra/src/main/java/com/eventitta/infra/gamification/service/RedisRankingService.java:60-103`
- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/repository/UserGamificationStatsRepository.java:17-35`

영향:
동점 유저가 여러 명이면 Redis 경로와 DB fallback 경로의 순서가 달라질 수 있습니다. 운영 중 Redis 장애나 warm-up 시 사용자에게 순위가 뒤바뀌어 보일 수 있습니다.

## P2

### 4. 0점 또는 0활동 유저가 Redis 랭킹에 남아 DB 기준과 어긋날 수 있다

실서비스 차이:
랭킹 참여 기준은 보통 하나여야 합니다. 현재 DB fallback은 `> 0` 유저만 포함하지만 Redis projection은 0점도 멤버로 남길 수 있습니다.

코드 근거:
DB fallback은 `totalPoints > 0`, `totalActivityCount > 0`만 집계합니다. 반면 Redis projection은 `updatePointsRanking`, `updateActivityCountRanking`에서 0일 때도 삭제하지 않고 ZSET에 그대로 넣습니다. `getTotalUsers`는 `zCard`를 그대로 반환합니다.

- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/repository/UserGamificationStatsRepository.java:22-23`
- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/repository/UserGamificationStatsRepository.java:32-33`
- `eventitta-infra/src/main/java/com/eventitta/infra/gamification/service/RedisRankingService.java:233-250`
- `eventitta-infra/src/main/java/com/eventitta/infra/gamification/service/RedisRankingService.java:300-318`

영향:
액션 revoke 후 점수가 0이 된 유저가 Redis 랭킹 참여자 수에는 남고, DB fallback에서는 사라질 수 있습니다. `totalUsers`, 내 순위, Top N 결과가 서로 다르게 보일 가능성이 있습니다.

### 5. 정지 사용자가 랭킹과 뱃지 평가에서 사실상 활성 사용자처럼 취급된다

실서비스 차이:
운영 서비스는 `deleted`뿐 아니라 `suspended`, 제재, moderation 상태를 리더보드와 리워드 정책에 반영합니다.

코드 근거:
`User`에는 `suspended` 필드가 있지만, `UserProfileView`는 `deleted`만 담고 `isActive()`도 `!deleted`만 확인합니다. Redis 랭킹 응답도 `user.isActive()`만 보고 필터링합니다.

- `eventitta-domain/src/main/java/com/eventitta/domain/user/domain/User.java:94-100`
- `eventitta-domain/src/main/java/com/eventitta/domain/user/api/internal/view/UserProfileView.java:3-12`
- `eventitta-domain/src/main/java/com/eventitta/domain/user/service/DefaultUserInternalFacade.java:216-226`
- `eventitta-infra/src/main/java/com/eventitta/infra/gamification/service/RedisRankingService.java:88-102`

영향:
정지 계정이 랭킹에 남거나, 정지 이전에 쌓은 보상이 그대로 노출될 수 있습니다. 운영 정책과 게이미피케이션 정책이 분리된 상태입니다.

### 6. 뱃지는 지급만 되고 사용자 경험 루프가 닫혀 있지 않다

실서비스 차이:
실서비스의 뱃지는 저장만으로 끝나지 않습니다. 조회 API, 프로필 노출, 획득 알림, 공유 가능한 상태가 같이 있어야 루프가 완성됩니다.

코드 근거:
공개 읽기 면은 랭킹 API와 `GET /api/v1/users/me/activities`뿐입니다. `UserProfileResponse`에도 포인트/레벨/뱃지 필드가 없습니다. `BadgeService`는 획득한 뱃지 이름 목록을 반환하지만, `BadgeProjectionListener`는 그 반환값을 어디에도 쓰지 않습니다. 저장 후 알림이나 응답 반영이 없습니다. 저장소 전체 검색에서도 badge controller/response나 포인트 이력 API는 찾지 못했습니다.

- `eventitta-api/src/main/java/com/eventitta/api/user/controller/UserController.java:184-187`
- `eventitta-api/src/main/java/com/eventitta/api/user/controller/response/UserProfileResponse.java:10-39`
- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/service/BadgeService.java:29-63`
- `eventitta-infra/src/main/java/com/eventitta/infra/gamification/event/BadgeProjectionListener.java:24-39`

영향:
현재 구조는 “유저가 배지를 얻는다”보다 “백엔드가 user_badges에 row를 저장한다”에 가깝습니다. 실서비스 기준으로는 보상 설계가 아니라 내부 집계 기능에 머뭅니다.

### 7. 취소 가능한 액션으로 딴 배지가 영구 유지된다

실서비스 차이:
삭제/취소 가능한 행동에 연동된 뱃지는 보통 회수 정책을 두거나, 최소한 immutable badge와 reversible badge를 분리합니다.

코드 근거:
회수 흐름은 action stats만 깎고, badge listener는 `checkAndAwardBadges`만 호출합니다. `BadgeService`는 이미 가진 배지를 만나면 바로 `continue`하며 회수 경로가 없습니다. 저장소 검색에서도 badge revoke/delete 로직은 없습니다.

- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/service/DefaultGamificationFacade.java:106-119`
- `eventitta-infra/src/main/java/com/eventitta/infra/gamification/event/BadgeProjectionListener.java:24-33`
- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/service/BadgeService.java:48-63`

영향:
좋아요를 누적했다가 모두 취소해도 뱃지는 유지됩니다. 모임 승인 후 취소 가능한 구조와 결합하면 “한 번만 조건 넘기면 영구 보유”가 되어 서비스 신뢰성을 떨어뜨립니다.

### 8. 포인트 변경 사유를 설명할 수 있는 ledger가 없다

실서비스 차이:
운영 서비스는 CS, 분쟁, abuse 분석을 위해 append-only ledger 또는 최소한 reasoned history를 둡니다.

코드 근거:
문서가 `user_activities`를 “현재 유효한 보상 기록”으로 정의하고 있고, 실제 revoke는 해당 row를 삭제합니다. 즉, 현재 구조는 “누적 이력”이 아니라 “현재 살아 있는 보상 집합”입니다.

- `docs/gamification/OVERVIEW.md:111-118`
- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/domain/GamificationActionRecord.java:15-18`
- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/service/DefaultGamificationFacade.java:106-119`
- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/repository/GamificationActionRecordRepository.java:12-16`

영향:
포인트가 왜 올랐고 왜 내려갔는지 사후 설명하기 어렵습니다. 사용자가 “점수가 왜 사라졌냐”고 물어도 현재 상태를 재계산하는 것 외에 정확한 히스토리를 제시하기 어렵습니다.

## P3

### 9. 게이미피케이션 규칙 표현력이 너무 단순하다

실서비스 차이:
실서비스는 streak, 기간 조건, 지역 조건, 품질 조건, 친구/지역 cohort, 시즌성 랭킹처럼 다축 규칙이 일반적입니다. 현재는 단일 액션 타입과 단일 threshold만 있습니다.

코드 근거:
`BadgeRule`은 `activityType`, `evaluationType`, `threshold`, `enabled`만 가집니다. `RewardActionType`도 4개 고정 액션뿐입니다. 랭킹 타입도 `POINTS`, `ACTIVITY_COUNT` 두 종류만 있습니다.

- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/domain/BadgeRule.java:13-32`
- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/domain/RewardActionType.java:7-24`
- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/domain/RankingType.java:10-18`

영향:
서비스가 커질수록 “많이 했는가” 외의 품질 신호를 붙이기 어렵습니다. 지역 커뮤니티 서비스인데도 전부 글로벌 집계라 상위권 고착이 쉽게 생깁니다.

### 10. `users.points`가 남아 있어 향후 잘못 읽을 위험이 있다

실서비스 차이:
운영 서비스는 점수 source of truth를 하나로 정리하거나, 호환 필드는 최소한 읽기 차단이나 동기화 규칙을 명확히 둡니다.

코드 근거:
문서는 `users.points`가 source of truth가 아니라고 적지만, 엔티티와 저장소에는 여전히 points 필드와 증감/정렬 메서드가 남아 있습니다.

- `docs/gamification/OVERVIEW.md:113`
- `eventitta-domain/src/main/java/com/eventitta/domain/user/domain/User.java:71-81`
- `eventitta-domain/src/main/java/com/eventitta/domain/user/repository/UserRepository.java:37-67`

영향:
현재 읽기 면이 작아서 즉시 터지는 문제는 아니지만, 후속 기능이나 운영 쿼리가 `users.points`를 참조하면 stats와 다른 값을 보게 될 수 있습니다. 지금은 잠재적 foot-gun에 가깝습니다.

## 현재 테스트가 보장하는 것과 비어 있는 것

현재 보장하는 것:

- facade 적립/회수와 중복 적립 방지
  `eventitta-app/src/test/java/com/eventitta/gamification/service/DefaultGamificationFacadeIntegrationTest.java`
- 다중 스레드 적립 시 total stats 누락 방지
  `eventitta-app/src/test/java/com/eventitta/gamification/service/GamificationFacadeConcurrencyTest.java`
- Redis 장애 시 랭킹 fallback
  `eventitta-infra/src/test/java/com/eventitta/infra/gamification/service/RankingServiceTest.java`
- 뱃지 기본 지급/중복 방지/포인트 기준 지급
  `eventitta-domain/src/test/java/com/eventitta/domain/gamification/service/BadgeServiceTest.java`
- 랭킹 API와 활동 요약 API의 기본 응답 형태
  `eventitta-app/src/test/java/com/eventitta/gamification/controller/RankingControllerTest.java`
  `eventitta-app/src/test/java/com/eventitta/user/controller/UserControllerTest.java`

현재 비어 있는 것:

- 동점 유저 Redis 순서와 DB fallback 순서 일치성
- 0점 또는 0활동 유저의 Redis eviction
- 정지 유저의 랭킹/뱃지 배제
- self-like 보상 차단
- 모임 승인 후 노쇼 시 보상 회수
- 취소 가능한 액션에 대한 뱃지 회수 정책
- 뱃지 획득 알림, 포인트/뱃지 조회 API, 포인트 변경 이력 조회

## 결론

- 현재 Eventitta 게이미피케이션은 “기본 적립/집계 엔진”으로서는 꽤 정리되어 있습니다.
- 하지만 지역 커뮤니티/모임 서비스 기준의 “실서비스 게이미피케이션”으로 보면, 보상 기준의 실제성, 악용 방지, 랭킹 일관성, 사용자 노출 루프, 운영 추적 가능성이 아직 부족합니다.
- 가장 먼저 터질 위험은 승인 기반 모임 포인트, self-like 허용, Redis 랭킹과 DB fallback 규칙 불일치입니다.
- 그 다음 단계의 문제는 배지가 내부 저장으로 끝나는 점, 취소 가능한 액션에도 영구 뱃지가 유지되는 점, point ledger 부재입니다.
