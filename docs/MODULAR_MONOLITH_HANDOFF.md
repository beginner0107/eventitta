# Modular Monolith Handoff

## Current branch

- `codex/modular-monolith-refactor`

## Current module layout

- `eventitta-app`
  - Spring Boot 조립, 실행 진입점, ArchUnit, 조립 관점 테스트
- `eventitta-api`
  - controller, security, request/response, mapper, cookie writer, web adapter
- `eventitta-domain`
  - use case, entity, repository port, internal facade contract, auth business logic
- `eventitta-infra`
  - JPA/Querydsl 구현, Redis, S3, RestClient, scheduler, config properties, Kakao REST adapter

## Allowed dependency direction

- `eventitta-app -> eventitta-api, eventitta-domain, eventitta-infra`
- `eventitta-api -> eventitta-domain`
- `eventitta-infra -> eventitta-domain`
- `eventitta-domain ->` no dependency on `api/infra/app`

## What is completed

### 1. Gradle ownership hardening

- 루트 `build.gradle` 은 버전, BOM, toolchain, 공통 test/annotation processor 규칙만 유지한다.
- 기술 스택 의존성은 각 모듈 build 파일이 직접 소유한다.
- `eventitta-domain` 에서 `web/security/openapi/redis/aws/flyway/rest client` 일괄 주입을 제거했다.

### 2. Domain purification

- `MultipartFile`, `FileDownloadResponse`, `@ConfigurationProperties`, `@GetExchange` 를 domain에서 제거했다.
- 파일 업로드 계약은 `UploadFileCommand`, 다운로드 계약은 `StoredFileView` 로 바꿨다.
- 미디어 정책은 `MediaPolicyProvider` 기반 도메인 정책 모델로 정리했다.
- 축제 외부 API client 와 runtime properties 는 infra 로 이동했다.
- 로컬 파일 저장 구현은 infra 로 이동했다.

### 3. Boundary hardening

- `region`
  - `RegionInternalFacade`, `RegionReferenceView` 추가
- `post`
  - `Post.user -> authorUserId`
  - `Post.region -> regionCode`
  - `PostImage.mediaAsset -> mediaAssetId`
  - `PostService` 가 `UserInternalFacade`, `RegionInternalFacade`, `MediaAssetInternalFacade`, `CommentQueryFacade` 를 사용하도록 정리
- `comment`
  - `Comment.post/user -> postId/userId`
  - `CommentService` 가 `PostInternalFacade`, `UserInternalFacade` 를 사용
  - `CommentQueryFacade` 추가
- `meeting`
  - `Meeting.leader -> leaderId`
  - `MeetingParticipant.user -> userId`
  - `MeetingService` 가 `UserInternalFacade` 기반으로 동작
- `gamification`
  - `UserBadge.user -> userId`
  - badge / ranking 흐름이 `UserInternalFacade` 기반으로 동작

### 4. Guardrails

- ArchUnit rules in:
  - `eventitta-app/src/test/java/com/eventitta/architecture/ModularStructureArchTest.java`
- 현재 주요 규칙:
  - `domain -> api/infra/app` 금지
  - `api -> infra` 금지
  - `infra -> api` 금지
  - `api.auth.service..` 같은 비즈니스 서비스 패키지 재도입 금지
  - domain에서 `MultipartFile`, `Resource`, `MediaType`, `@ConfigurationProperties`, `@GetExchange` 사용 금지
  - `region/post/comment/meeting/gamification` 은 foreign repository/service/entity 직접 참조 금지

### 5. Test ownership migration

- controller test moved to `eventitta-api`
- domain unit/service test moved to `eventitta-domain`
- infra adapter/service test moved to `eventitta-infra`
- `eventitta-app` 에는 조립 관점과 남은 legacy 테스트만 둔다

### 6. Auth boundary refactor

- 로컬 로그인/회원가입/refresh/logout/Kakao 로그인 링크 비즈니스 로직을 `eventitta-domain.auth..` 로 이동했다.
- `eventitta-api` 에는 `AuthController`, `UserController`, `AuthMapper`, `JwtTokenProvider`, `AuthenticationManagerCredentialAuthenticator`, `CookieManager`, `KakaoAuthorizationSupport` 만 남겼다.
- `eventitta-infra` 는 `JpaRefreshTokenRepository`, `JpaAuthIdentityRepository`, `KakaoAuthClientAdapter` 로 저장소/외부 연동을 소유한다.
- `refresh_tokens` 는 더 이상 `findAllByUserId + PBKDF2 scan` 으로 조회하지 않고, 인덱스된 `token_key` 로 한 건을 찾은 뒤 secret 해시만 검증한다.
- refresh cookie 원문은 `<tokenKey>.<secret>` 형식이며, 회전(refresh) 시 기존 row 를 삭제하고 새 row 를 발급한다.
- `auth_identity` 테이블을 추가해 소셜 로그인 기준 데이터를 `provider + provider_user_id` 로 관리한다.
- `users.provider/provider_id` 는 legacy 호환 필드로만 남고, 새 소셜 인증 플로우에서는 신뢰 소스로 사용하지 않는다.
- access cookie `Max-Age` 는 access TTL, refresh cookie `Max-Age` 는 refresh TTL 을 각각 사용한다.

### 7. Kakao social login surface

- 추가 엔드포인트:
  - `POST /api/v1/auth/social/kakao/authorize`
  - `POST /api/v1/auth/social/kakao/login`
  - `POST /api/v1/users/me/social/kakao/link`
- `authorize` 는 redirect URI allowlist 검증 후 짧은 수명의 `oauth_state` HttpOnly cookie 와 authorize URL 을 내려준다.
- `login` 은 `state` 검증, code 교환, 사용자 정보 재조회, `provider user id + verified email` 검증 뒤 로그인한다.
- 이미 연결된 Kakao identity 가 있으면 즉시 로그인한다.
- 기존 로컬 유저 이메일과 충돌하면 자동 링크하지 않고 `SOCIAL_LINK_REQUIRED` 를 반환한다.
- 로그인된 로컬 세션에서만 `link` 를 호출할 수 있고, 현재 사용자 이메일과 Kakao verified email 이 같을 때만 identity 를 생성한다.

## Important design rules

- 외부 REST API shape 는 유지한다.
- 쓰기 경로는 `api.internal + scalar ID/code` 중심으로 유지한다.
- 읽기 경로는 응답 호환성을 위해 infra projection join 을 일부 허용한다.
- foreign domain interaction 은 `api.internal..` contract 를 우선 사용한다.
- 웹 세션 전달은 계속 `HttpOnly Cookie only` 로 유지한다.
- social login 은 `same-site web` 전제를 기준으로 설계하고, CORS 는 명시적 allowlist 만 허용한다.

## Known remaining work

- `eventitta-app` 에 남아 있는 legacy 테스트를 모듈 소유 기준으로 추가 이관해야 한다.
- `media -> post`, `user -> auth/meeting`, 일부 read-side query 는 아직 후속 경계 정리가 더 필요하다.
- touched scope 기준 테스트 이관은 진행됐지만 전체 테스트 스위트의 모듈 ownership 정리는 아직 끝나지 않았다.
- Apple login, 모바일 네이티브 플로우, Kakao unlink/webhook 동기화는 이번 트랜치 범위 밖이다.
- 운영 환경 redirect URI/origin allowlist 는 배포 환경별 값 검증이 필요하다.

## Safe next-session checks

- `git switch codex/modular-monolith-refactor`
- `./gradlew compileJava`
- `./gradlew testClasses`
- `./gradlew test`
- `./gradlew :eventitta-api:test --tests '*AuthControllerTest' --tests '*UserControllerSocialLinkTest'`
- `./gradlew :eventitta-domain:test --tests '*AuthServiceTest' --tests '*RefreshTokenServiceTest' --tests '*TokenServiceTest'`
- `./gradlew :eventitta-infra:test --tests '*RefreshTokenRepositoryTest' --tests '*AuthIdentityRepositoryTest' --tests '*KakaoAuthClientAdapterTest'`
- `rg --pcre2 "import com\\.eventitta\\.domain\\.[^.]+\\.(repository|service)\\." eventitta-domain/src/main/java`
- `rg "MultipartFile|Resource|MediaType|@ConfigurationProperties|@GetExchange" eventitta-domain/src/main/java`

## Practical next tranche

1. 남아 있는 legacy app 테스트를 `api/domain/infra` 로 추가 이관
2. `media -> post` 및 `user -> auth/meeting` foreign dependency를 internal API로 축소
3. social unlink / 계정 해제 동기화 / Apple provider 를 후속 설계로 분리
4. `./gradlew test` 기준 전체 회귀 안정화
