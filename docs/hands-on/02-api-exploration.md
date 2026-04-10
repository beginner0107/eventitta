# 2. API 실습 탐색

이 문서는 실제 API를 호출하면서 코드와 런타임 변화를 같이 보는 순서를 정리합니다.

실행 자산:

- [../http/auth-user-session.http](../http/auth-user-session.http)
- [../http/meeting-region-ranking-admin.http](../http/meeting-region-ranking-admin.http)

## 실습 전에 같이 켜둘 것

```bash
tail -f ./logs/eventitta.log
```

```bash
curl http://localhost:8080/actuator/health
```

MySQL과 Redis를 같이 보고 싶다면 [04-sql-cache-redis-observability.md](./04-sql-cache-redis-observability.md)의 명령도 같이 준비합니다.

## 흐름 A. 인증과 사용자 세션

기본 실행 파일은 [../http/auth-user-session.http](../http/auth-user-session.http) 입니다.

| 순서 | 엔드포인트 | 목적 | 기대 결과 | DB/상태 변화 | 함께 읽을 코드 | 로그 포인트 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `POST /api/v1/auth/signup` | 신규 사용자 생성 | `200 OK`, `email`, `nickname` 반환 | `users` insert | `AuthController`, `AuthService`, `AuthControllerTest` | `users` insert SQL |
| 2 | `POST /api/v1/auth/login` | access/refresh cookie 발급 | `200 OK`, `Set-Cookie` 2개 | `refresh_tokens` insert 또는 회전용 저장 | `AuthController`, `CookieManager`, `SecurityConfig` | `refresh_tokens` 관련 SQL |
| 3 | `GET /api/v1/users/me` | 인증된 사용자 프로필 확인 | `200 OK`, 현재 사용자 정보 | 읽기 전용 | `UserController`, `UserService` | 인증 후 profile 조회 SQL |
| 4 | `GET /api/v1/users/me/sessions` | 현재 계정 세션 목록 확인 | `200 OK`, `sessionId`, `issuedAt`, `lastSeenAt` 등 반환 | `refresh_tokens` read | `UserController`, `AuthService`, `UserSessionResponse` | 세션 조회 SQL |
| 5 | `POST /api/v1/auth/refresh` | 쿠키 회전 확인 | `200 OK`, 새 `Set-Cookie` 발급 | 기존 refresh token 교체 | `AuthController`, `AuthService`, `RefreshTokenRepositoryTest` | `findByTokenKeyForUpdate`, delete/save SQL |
| 6 | `DELETE /api/v1/users/me/sessions/{sessionId}` 또는 `DELETE /api/v1/users/me/sessions` | 세션 종료 흐름 이해 | `204 No Content` | refresh token row 삭제 | `UserController`, `AuthService` | 세션 삭제 SQL |
| 7 | `POST /api/v1/auth/logout` | 현재 세션 종료 및 쿠키 제거 | `204 No Content` | 현재 refresh token 제거 | `AuthController`, `CookieManager` | logout 이후 cookie 삭제, refresh token 정리 |

### 이 흐름에서 꼭 볼 것

- `access_token`, `refresh_token` 쿠키가 어디에서 쓰이고 어디에서 지워지는지
- `SecurityConfig`와 `JwtAuthenticationFilter`가 인증 정보를 언제 세팅하는지
- `refresh_tokens` 테이블이 refresh 시점에 어떻게 바뀌는지
- `GET /api/v1/users/me/sessions`의 `current` 플래그가 어떤 세션을 가리키는지

### 확장 탐색 포인트

- `PUT /api/v1/users/me/password`
- `POST /api/v1/users/me/password/local`
- `POST /api/v1/auth/email-verification/request`
- `POST /api/v1/auth/password-reset/request`

위 엔드포인트들은 로그인/계정 보안 쪽 경계를 더 보고 싶을 때 이어서 확인합니다.

## 흐름 B. 지역 조회와 캐시

기본 실행 파일은 [../http/meeting-region-ranking-admin.http](../http/meeting-region-ranking-admin.http) 입니다.

| 순서 | 엔드포인트 | 목적 | 기대 결과 | DB/캐시 변화 | 함께 읽을 코드 | 로그 포인트 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `GET /api/v1/regions` | 최상위 지역 목록 확인 | `200 OK`, 시/도 목록 | 최초 1회 `regions` 캐시 채움 | `RegionController`, `RegionService`, `RegionCacheService` | `캐시 미스 - DB에서 전체 지역 데이터 로드` |
| 2 | `GET /api/v1/regions/{parentCode}` | 하위 지역 탐색 | `200 OK`, 하위 지역 목록 | 캐시 hit 중심 | 같은 코드 | 첫 호출 이후 SQL 감소 여부 |
| 3 | `GET /api/v1/regions/options` | 전체 leaf region 옵션 조회 | `200 OK`, `hierarchyCodes`, `hierarchyNames` 포함 | `regionOptions` 캐시 채움 | `RegionService.getRegionOptions()` | 첫 호출과 재호출의 로그 차이 |

### 이 흐름에서 꼭 볼 것

- 앱 시작 직후 `RegionCacheService.warmUpCache()` 로그
- `regions`, `regionOptions`가 다른 목적의 캐시라는 점
- 지역 API는 `local`/운영 모두 GET 기준 public path로 열려 있다는 점

## 흐름 C. 모임 생성, 참가, 승인

같은 파일 [../http/meeting-region-ranking-admin.http](../http/meeting-region-ranking-admin.http) 안에 leader/participant 흐름이 나뉘어 있습니다.

| 순서 | 엔드포인트 | 목적 | 기대 결과 | DB 변화 | 함께 읽을 코드 | 로그 포인트 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `POST /api/v1/auth/signup`, `POST /api/v1/auth/login` (leader) | 모임장 세션 준비 | leader 쿠키 확보 | `users`, `refresh_tokens` | `AuthController`, `SecurityConfig` | auth SQL |
| 2 | `POST /api/v1/meetings` | 모임 생성 | `201 Created`, `Location` 헤더 반환 | `meetings` insert | `MeetingController`, `MeetingService`, `MeetingCreateRequest` | meeting insert SQL |
| 3 | `GET /api/v1/meetings`, `GET /api/v1/meetings/{meetingId}` | 목록/상세 확인 | `PageResponse`, `MeetingDetailResponse` | 읽기 전용 | `MeetingController` | 조회 SQL |
| 4 | `POST /api/v1/auth/signup`, `POST /api/v1/auth/login` (participant) | 참가자 세션 준비 | participant 쿠키 확보 | `users`, `refresh_tokens` | auth 계열 | auth SQL |
| 5 | `POST /api/v1/meetings/{meetingId}/join` | 참가 신청 | `participantId`, `status=PENDING` | `meeting_participants` insert | `MeetingController`, `MeetingService`, `JoinMeetingResponse` | join insert SQL |
| 6 | `PUT /api/v1/meetings/{meetingId}/participants/{participantId}/approve` | 참가 승인 | `200 OK`, `status=APPROVED` | `meeting_participants` update, 모임 인원수 반영 | `MeetingController`, `MeetingService` | approve update SQL |

### 이 흐름에서 꼭 볼 것

- 인증이 필요한 요청과 public 조회 요청이 어떻게 갈리는지
- `Location` 헤더를 통해 생성 결과를 후속 조회에 연결하는 방식
- 참가/승인 흐름이 leader와 participant 두 세션을 전제로 한다는 점
- 스케줄러가 이후 종료 시점을 만나면 `MeetingStatusScheduler`가 상태를 바꾼다는 점

## 흐름 D. 랭킹 조회와 Redis 읽기

같은 파일 [../http/meeting-region-ranking-admin.http](../http/meeting-region-ranking-admin.http) 에 랭킹 조회 요청이 들어 있습니다.

| 순서 | 엔드포인트 | 목적 | 기대 결과 | Redis/DB 포인트 | 함께 읽을 코드 | 로그 포인트 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `GET /api/v1/rankings/top?type=POINTS&limit=10` | 포인트 상위 랭킹 확인 | `200 OK`, top N 목록 | `ranking:points` | `RankingController`, `RankingService`, `RedisRankingService` | ranking 조회 로그 |
| 2 | `GET /api/v1/rankings/me?type=POINTS` | 내 랭킹 확인 | `200 OK` | 같은 Redis key | `RankingController` | 인증된 userId 기반 조회 |
| 3 | `GET /api/v1/rankings/stats` | 랭킹 참여자 수 확인 | `200 OK`, count 응답 | `ranking:points`, `ranking:activity:count` | `RankingController` | Redis total user count 조회 |

### 이 흐름에서 꼭 볼 것

- 랭킹 API는 `local`에서도 인증이 필요하다는 점
- Redis key가 enum `RankingType`에 직접 매핑되어 있다는 점
- Redis 장애 시 어떤 fallback이 있는지 읽고 싶다면 `RedisRankingService` 구현을 바로 확인할 것

## 흐름 E. 관리자 축제 수동 sync

실행 파일: [../http/meeting-region-ranking-admin.http](../http/meeting-region-ranking-admin.http)

| 엔드포인트 | 목적 | 주의 사항 | 함께 읽을 코드 | 로그 포인트 |
| --- | --- | --- | --- | --- |
| `POST /api/v1/admin/festivals/sync/national` | 전국 축제 수동 sync | `FestivalAdminController`에 `@PreAuthorize("hasRole('ADMIN')")`가 있어 관리자 계정 필요 | `FestivalAdminController`, `FestivalService`, `FestivalScheduler` | `[Admin]`, `[Scheduler]`, 외부 API 호출 로그 |
| `POST /api/v1/admin/festivals/sync/seoul` | 서울시 축제 수동 sync | 운영과 동일하게 관리자 권한을 전제로 본다 | 같은 코드 | sync 건수, 실패 로그 |

### 스케줄러와 수동 API의 차이

- 수동 API는 HTTP를 통해 즉시 호출합니다.
- 스케줄러는 `scheduler.*.enabled`와 cron에 따라 자동 실행됩니다.
- 둘 다 내부적으로는 `FestivalService`를 통해 도메인 로직으로 들어갑니다.

## 추천 실습 순서

1. `auth-user-session.http`를 위에서 아래로 실행한다.
2. `meeting-region-ranking-admin.http`에서 지역 조회와 leader 모임 생성까지 실행한다.
3. participant 흐름으로 join 후 leader 흐름으로 approve 한다.
4. 랭킹 API를 호출한 뒤 Redis 키를 확인한다.
5. 관리자 계정이 있으면 festival sync를 수동 호출하고 로그를 본다.
