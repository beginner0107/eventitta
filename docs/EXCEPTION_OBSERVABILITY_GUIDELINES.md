# 예외 관측성 규약

## 목적

이 문서는 Eventitta 백엔드의 예외 처리 시 다음 항목을 일관되게 유지하기 위한 운영 규약을 정의한다.

- HTTP 응답 규약
- 서버 로그 규약
- Discord 알림 규약
- 원인 분석을 위한 추적 필드 규약

핵심 원칙은 다음과 같다.

1. 외부 응답은 단순하고 안정적으로 유지한다.
2. 내부 로그와 알림은 원인 분석에 필요한 정보를 충분히 남긴다.
3. 동일한 성격의 예외는 동일한 수준의 로그/알림 정책을 따른다.
4. 알림 실패는 원래 예외 응답을 깨지 않아야 한다.

---

## 적용 범위

이 규약은 아래 경로에 적용한다.

- `GlobalExceptionHandler`
- Spring Security 예외 진입점
  - `AuthenticationEntryPoint`
  - `AccessDeniedHandler`
- 스케줄러/비동기 이벤트 처리 중 최상위 예외 로깅
- 외부 의존성 호출 실패 후 최종 예외 처리 지점

도메인 서비스 내부에서 예외를 발생시키는 방식 자체보다는, 최종적으로 예외를 관측하는 계층의 일관성에 초점을 둔다.

---

## 예외 분류

### 1. 사용자 요청 오류

대상:

- 요청 본문 검증 실패
- 파라미터 누락/타입 오류
- 비즈니스 검증 실패
- 존재하지 않는 리소스 조회

대표 상태 코드:

- `400 Bad Request`
- `404 Not Found`
- `409 Conflict`

운영 해석:

- 정상적인 시스템 동작 범위 내의 예외
- 기본적으로 서버 장애로 간주하지 않음

### 2. 인증/인가 오류

대상:

- 인증 실패
- 만료/변조된 토큰
- 로그인 실패
- Spring Security `AccessDeniedException`
- 도메인 권한 부족 예외

대표 상태 코드:

- `401 Unauthorized`
- `403 Forbidden`

운영 해석:

- 보안 관련 이벤트
- 단건 자체는 서버 장애가 아니지만, 반복 패턴은 운영상 중요할 수 있음

### 3. 상태 충돌

대상:

- 낙관적/비관적 락 타임아웃
- 현재 상태상 수행 불가
- 중복 요청

대표 상태 코드:

- `409 Conflict`

운영 해석:

- 시스템 장애는 아니지만 동시성/사용 패턴 관점에서 관찰 가치가 있음

### 4. 데이터 무결성/DB 이상

대상:

- `DataIntegrityViolationException`
- 예상하지 못한 unique constraint 위반
- FK/NOT NULL/DDL 제약 위반

대표 상태 코드:

- 원칙적으로 `500 Internal Server Error`

운영 해석:

- 사전 검증을 통과했는데 발생했다면 서버 버그 또는 비정상 상태로 간주
- 일반 4xx 충돌과 구분해야 함

### 5. 외부 의존성 장애

대상:

- DB 연결 실패
- Redis 연결 실패
- 외부 API 연결 실패
- 네트워크 타임아웃/연결 거부

대표 상태 코드:

- `500 Internal Server Error`
- `503 Service Unavailable`

운영 해석:

- 운영자가 바로 인지해야 하는 장애 후보

### 6. 미분류 서버 오류

대상:

- 위 어느 항목에도 속하지 않는 `Exception`

대표 상태 코드:

- `500 Internal Server Error`

운영 해석:

- 원인 불명 서버 오류
- 우선순위 높은 관측 대상

---

## 응답/로그/알림 매트릭스

| 예외 유형 | 응답 코드 | 로그 레벨 | Discord 알림 | 비고 |
|-----------|-----------|-----------|---------------|------|
| 검증 실패/입력 오류 | 400 | `WARN` 또는 생략 | 기본 미전송 | 반복 패턴은 별도 고려 |
| 리소스 없음 | 404 | `INFO` 또는 `WARN` | 미전송 | 정상 범위 가능 |
| 도메인 권한 부족 | 403 | `WARN` | 기본 미전송 | 사용자 행위 추적용 |
| 인증 실패/토큰 오류 | 401 | `WARN` | 상황별 전송 | 반복 시 보안 이벤트 |
| 상태 충돌/락 타임아웃 | 409 | `WARN` | 상황별 전송 | 과도한 빈도면 튜닝 대상 |
| 데이터 무결성 위반 | 500 | `ERROR` | `HIGH` 전송 | constraint 정보 필수 |
| 외부 의존성 장애 | 500/503 | `ERROR` | `HIGH` 또는 `CRITICAL` | 연결/타임아웃 구분 |
| 미분류 서버 오류 | 500 | `ERROR` | `HIGH` 전송 | 공통 fallback |

---

## 로그 규약

### 공통 필수 필드

모든 최상위 예외 로그는 가능하면 아래 필드를 동일 키로 남긴다.

- `errorCode`
- `exceptionType`
- `path`
- `httpStatus`
- `userInfo`
- `rootCauseType`
- `rootCauseMessage`

### 구현 예정 필드

아래 필드는 아직 구현돼 있지 않더라도 규약상 필수 필드로 본다.

- `traceId`
- `requestId`
- `clientIp`

추후 MDC 또는 필터 기반 요청 식별자를 붙여 로그/알림 양쪽에 동일하게 전달한다.

### 예외 유형별 추가 필드

#### 데이터 무결성 위반

- `constraintName`
- `sqlState`
- `vendorCode`
- `mostSpecificCauseType`
- `mostSpecificCauseMessage`

#### 인증/인가

- `authType`
- `principal`
- `requiredRole`
- `grantedAuthorities`

#### 외부 의존성 장애

- `dependency`
- `endpoint`
- `timeoutMs`
- `retryCount`

### 로그 메시지 원칙

1. 메시지는 예외 이름이 아니라 운영 해석 중심으로 쓴다.
2. 검색 가능한 고정 접두어를 둔다.
3. 동일 유형 예외는 동일한 메시지 패턴을 사용한다.

예시:

- `"[Exception] Validation failure ..."`
- `"[Exception] Authentication failure ..."`
- `"[Exception] Access denied ..."`
- `"[Exception] Data integrity violation ..."`
- `"[Exception] Internal server error ..."`

---

## Discord 알림 규약

### 알림 레벨 기준

| 상황 | 레벨 |
|------|------|
| 일반 검증 실패, 단순 4xx | 전송 안 함 |
| 반복되는 권한/인증 실패, 의심스러운 요청 패턴 | `MEDIUM` |
| 일반 5xx, 데이터 무결성 위반 | `HIGH` |
| DB/네트워크 핵심 장애, 시스템 전반 영향 | `CRITICAL` |

### 알림 포함 필드

- `errorCode`
- `path`
- `userInfo`
- `exceptionType`
- `rootCauseMessage`
- `traceId/requestId` 구현 시 포함

### 알림 fail-safe 원칙

1. Discord 알림 실패는 원래 HTTP 응답에 영향을 주면 안 된다.
2. 알림 실패 자체는 별도 `ERROR` 로그로 남긴다.
3. 원래 예외와 알림 실패 예외를 함께 식별 가능해야 한다.

---

## Security 예외 규약

### 401 Unauthorized

대상:

- 인증되지 않은 사용자 접근
- 만료/변조/잘못된 액세스 토큰

규약:

- JSON 응답 포맷은 일반 API 에러 응답과 동일하게 유지
- 로그는 `WARN`
- 과도한 반복 시 Discord `MEDIUM` 이상 고려

### 403 Forbidden

대상:

- Spring Security `AccessDeniedException`
- 도메인 권한 부족

규약:

- Security 필터 체인에서 발생한 403도 일반 API 에러 응답과 동일한 JSON 형식으로 반환
- 로그는 `WARN`
- 사용자 식별 정보와 요청 경로를 남긴다

### 중요 원칙

Spring Security 경로에서 발생한 401/403도 `GlobalExceptionHandler` 밖이라고 해서 별도 형식으로 흩어지면 안 된다.  
응답 포맷, 로그 필드, 추적 키는 동일 규약을 따라야 한다.

---

## DataIntegrityViolationException 규약

`DataIntegrityViolationException`은 특별 취급한다.

### 분류 원칙

사전 중복/검증 로직을 통과한 뒤 발생한 `DataIntegrityViolationException`은 일반적인 비즈니스 충돌로 번역하지 않는다.  
운영 관점에서는 데이터 무결성 이상 또는 서버 버그 후보로 취급한다.

### 처리 원칙

- 응답: `500 Internal Server Error`
- 로그: `ERROR`
- 알림: `HIGH`
- 필수 필드: `constraintName`, `sqlState`, `vendorCode`

### 금지 사항

- 특정 도메인 에러 코드로 임의 번역하여 409로 숨기기
- 상세 원인 로그 없이 generic 500만 반환하기

---

## 공통 구현 원칙

### 1. 응답 생성과 관측성 처리는 분리

핸들러는 아래 두 단계를 명확히 분리한다.

1. 클라이언트 응답 결정
2. 로그/알림 전송

응답 생성 로직은 로그/알림 실패와 독립적이어야 한다.

### 2. 최상위 예외 처리 지점에서만 최종 로그를 남긴다

같은 예외를 서비스, 핸들러, 필터에서 중복으로 `ERROR` 로그 남기지 않는다.  
최종 책임 지점 하나에서만 장애 로그를 남기고, 중간 계층은 필요 시 `WARN` 또는 컨텍스트 로그만 남긴다.

### 3. 비즈니스 예외와 장애 예외를 분리한다

- 비즈니스 예외: 사용자 시나리오의 일부
- 장애 예외: 운영자가 봐야 하는 이상 상태

둘을 같은 수준으로 알리면 노이즈가 증가한다.

---

## 테스트 규약

예외 관측성 관련 테스트는 아래 수준으로 나눈다.

### 단위 테스트

- 알림 실패 시 원래 응답 유지
- 알림 레벨 결정 규칙 검증
- 보안 401/403 응답 포맷 검증

### 슬라이스 테스트

- `GlobalExceptionHandler`가 예외별 응답 코드를 올바르게 반환하는지 검증
- Security 필터 체인의 `AuthenticationEntryPoint`, `AccessDeniedHandler` 응답 검증

### 통합 테스트

- 실제 예외 발생 시 로그/알림 호출 여부
- 구현 후 `traceId/requestId`가 응답 없이 내부 추적에는 포함되는지 검증

---

## 단계별 적용 순서

1. 이 문서의 예외 분류/매트릭스를 기준 규약으로 확정한다.
2. `GlobalExceptionHandler`에 공통 관측성 헬퍼를 도입한다.
3. `AuthenticationEntryPoint`와 `AccessDeniedHandler`를 동일 규약에 맞춘다.
4. `AlertLevelResolver`를 이 문서의 등급 정책에 맞게 정리한다.
5. `traceId/requestId`를 도입해 로그와 알림을 연결한다.
6. 예외 유형별 테스트를 추가한다.

---

## 현재 코드베이스 기준 우선 정비 대상

현재 구조를 기준으로 우선순위가 높은 항목은 다음과 같다.

1. 예외 핸들러와 Security 경로의 로그/알림 정책 통일
2. `DataIntegrityViolationException`의 구조화 로그 유지
3. 인증/인가 실패 로그 필드 보강
4. `traceId/requestId` 도입
5. `AlertLevelResolver` 정책 재정렬

이 문서는 이후 관측성 관련 리팩토링의 기준 문서로 사용한다.
