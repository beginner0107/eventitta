# 1. 모듈 구조와 요청 흐름

이 문서는 "어디서 읽기 시작해야 하는가"를 정리합니다.

## 현재 모듈 구조

```text
eventitta
├── eventitta-app
├── eventitta-api
├── eventitta-domain
└── eventitta-infra
```

## 모듈 책임

| 모듈 | 책임 | 대표 관심사 |
| --- | --- | --- |
| `eventitta-app` | Spring Boot 조립과 부트스트랩 | 실행 진입점, ArchUnit, 조립 관점 테스트 |
| `eventitta-api` | 웹 어댑터 | controller, request/response, security, cookie, JWT filter |
| `eventitta-domain` | 비즈니스 로직 | entity, use case service, repository port, 내부 facade |
| `eventitta-infra` | 외부 기술 구현 | JPA/Querydsl, Redis, Scheduler, S3, 외부 HTTP client |

허용 의존 방향은 다음과 같습니다.

- `eventitta-app -> eventitta-api, eventitta-domain, eventitta-infra`
- `eventitta-api -> eventitta-domain`
- `eventitta-infra -> eventitta-domain`
- `eventitta-domain ->` 다른 모듈 의존 없음

상세 규칙은 [../ARCHITECTURE.md](../ARCHITECTURE.md)를 먼저 참고하면 좋습니다.

## 가장 먼저 열어볼 클래스

### 부트스트랩

- `eventitta-app/src/main/java/com/eventitta/app/EventittaApplication.java`
- `eventitta-app/src/main/resources/application-local.yml`

### 인증 흐름

- `eventitta-api/src/main/java/com/eventitta/api/auth/controller/AuthController.java`
- `eventitta-api/src/main/java/com/eventitta/api/auth/jwt/filter/JwtAuthenticationFilter.java`
- `eventitta-api/src/main/java/com/eventitta/api/auth/config/SecurityConfig.java`
- `eventitta-domain/src/main/java/com/eventitta/domain/auth/service/AuthService.java`
- `eventitta-infra/src/main/java/com/eventitta/infra/auth/repository/...`

### 모임 흐름

- `eventitta-api/src/main/java/com/eventitta/api/meeting/controller/MeetingController.java`
- `eventitta-domain/src/main/java/com/eventitta/domain/meeting/service/MeetingService.java`
- `eventitta-domain/src/main/java/com/eventitta/domain/meeting/repository/...`
- `eventitta-infra/src/main/java/com/eventitta/infra/meeting/...`

### 지역/캐시 흐름

- `eventitta-api/src/main/java/com/eventitta/api/region/controller/RegionController.java`
- `eventitta-domain/src/main/java/com/eventitta/domain/region/service/RegionService.java`
- `eventitta-domain/src/main/java/com/eventitta/domain/region/service/RegionCacheService.java`

### 랭킹/Redis 흐름

- `eventitta-api/src/main/java/com/eventitta/api/gamification/controller/RankingController.java`
- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/service/RankingService.java`
- `eventitta-infra/src/main/java/com/eventitta/infra/gamification/service/RedisRankingService.java`

## 실제 요청은 어떻게 흐르는가

대표적인 읽기 순서는 아래처럼 잡으면 됩니다.

1. Controller에서 endpoint와 인증 요구사항을 본다.
2. Request/Response DTO와 mapper를 본다.
3. Domain service에서 실제 유스케이스를 본다.
4. Domain repository port 또는 internal facade 호출을 본다.
5. Infra 구현에서 JPA/Redis/외부 API 접근을 본다.
6. 테스트에서 기대 동작과 경계 조건을 본다.

### 예시 1. 로그인 요청

```text
POST /api/v1/auth/login
  -> AuthController
  -> ClientSessionMetadataResolver / CookieManager
  -> AuthService.login(...)
  -> RefreshTokenRepository 저장
  -> 응답 쿠키(access_token, refresh_token) 발급
```

여기서 눈여겨볼 포인트:

- JWT 검증 책임은 filter 쪽에 있다.
- refresh token은 stateless가 아니라 DB에 `token_key + secret hash` 형태로 저장된다.
- API 계층은 cookie/write 책임을 갖고, 도메인은 토큰 생성/회전 규칙을 가진다.

### 예시 2. 모임 생성 요청

```text
POST /api/v1/meetings
  -> MeetingController
  -> MeetingService.createMeeting(...)
  -> MeetingRepository 저장
  -> 201 Created + Location 헤더
```

여기서 눈여겨볼 포인트:

- `@CurrentUser`가 붙은 userId는 API 계층에서 인증 결과를 주입받는다.
- domain service가 생성 규칙과 상태 전이를 관리한다.
- infra는 저장소 구현과 Querydsl 조회를 담당한다.

### 예시 3. 지역 옵션 조회

```text
GET /api/v1/regions/options
  -> RegionController
  -> RegionService.getRegionOptions()
  -> RegionCacheService.getAllRegionsAsMap()
  -> 최초 1회 DB 조회 후 Caffeine 캐시 사용
```

여기서 눈여겨볼 포인트:

- `regions`, `regionOptions` 두 캐시가 분리되어 있다.
- 앱 시작 직후 `ApplicationReadyEvent`로 지역 캐시 워밍업이 실행된다.

## 모듈 경계에서 보는 포인트

### `eventitta-api`

- HTTP 세부사항을 알고 있어야 하는 책임만 둡니다.
- 예: cookie, JWT filter, request validation, OpenAPI annotation

### `eventitta-domain`

- 기술 의존 없이 유스케이스를 설명해야 합니다.
- 다른 도메인 쓰기 경로는 `api.internal..` contract 또는 facade를 통해 접근합니다.

### `eventitta-infra`

- JPA, Redis, Scheduler, 외부 API처럼 "기술이 들어가는 구현"이 모여 있습니다.
- 같은 유스케이스라도 읽기(Querydsl projection)와 쓰기(repository 구현)가 같이 보일 수 있습니다.

## 테스트를 읽는 순서

권장 소유권은 다음과 같습니다.

- `eventitta-api`: controller / validation / web adapter 테스트
- `eventitta-domain`: entity / use case / domain service 테스트
- `eventitta-infra`: repository / Redis / external adapter 테스트
- `eventitta-app`: 부트/조립/ArchUnit 테스트

다만 현재는 legacy 테스트가 일부 `eventitta-app`에 남아 있습니다.

대표 예시:

- `eventitta-api/src/test/java/com/eventitta/api/auth/controller/AuthControllerTest.java`
- `eventitta-app/src/test/java/com/eventitta/meeting/controller/MeetingControllerTest.java`
- `eventitta-app/src/test/java/com/eventitta/region/controller/RegionControllerTest.java`
- `eventitta-app/src/test/java/com/eventitta/scheduler/ShedLockIntegrationTest.java`

즉, "이상적인 소유권"과 "현재 남아 있는 테스트 위치"가 완전히 일치하지는 않습니다. 문서를 읽을 때는 이 차이를 전제로 보는 편이 빠릅니다.

## 다음 단계

다음 문서인 [02-api-exploration.md](./02-api-exploration.md)에서는 실제 요청 순서대로 API를 호출하며 이 구조를 따라갑니다.
