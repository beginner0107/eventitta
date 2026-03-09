# DTO 배치 및 네이밍 가이드

## 목적

이 문서는 Eventitta에서 DTO를 어디에 두고, 어떤 이름으로 작성할지에 대한 기준을 정리한다.

핵심 원칙은 두 가지다.

- DTO는 `dto` 폴더에 무분별하게 모으지 않는다.
- DTO는 "누가 만들고 누가 소비하는가"를 기준으로 배치한다.

## 기본 원칙

### 1. 계층 소유권 기준으로 배치한다

DTO는 기술 용어가 아니라 소유 계층과 역할이 먼저 드러나야 한다.

- 웹 요청/응답 DTO는 `controller` 아래에 둔다.
- 유스케이스 입력/출력 DTO는 `service` 아래에 둔다.
- DB 조회 최적화를 위한 projection은 `repository/projection` 아래에 둔다.
- 도메인 개념이면 DTO로 두지 말고 `domain` 아래 모델로 둔다.

즉, `dto`는 중심 패키지가 아니라 보조 분류 정도로만 사용한다.

## 권장 패키지 구조

예시는 `auth` 도메인을 기준으로 한다.

```text
com.eventitta.auth
├── controller
│   ├── request
│   │   └── SignUpRequest
│   └── response
│       └── SignUpResponse
├── service
│   └── dto
│       ├── SignUpCommand
│       └── SignUpResult
└── repository
    └── projection
        └── UserSummaryProjection
```

## 네이밍 규칙

이름은 `Dto` 접미사보다 역할이 먼저 드러나야 한다.

- 컨트롤러 입력은 `...Request`
- 컨트롤러 출력은 `...Response`
- 서비스 입력은 `...Command`
- 서비스 출력은 `...Result`
- 리포지토리 projection은 `...Projection`

예시:

- `SignUpRequest`
- `SignUpResponse`
- `SignUpCommand`
- `SignUpResult`
- `UserSummaryProjection`

## `Dto` 접미사 사용 원칙

기본적으로 `Dto` 접미사는 붙이지 않는다.

다음과 같은 경우에만 예외적으로 허용할 수 있다.

- 외부 시스템 계약명과 맞춰야 하는 경우
- 같은 패키지 안에서 도메인 타입과 이름 충돌이 심한 경우
- 기존 레거시 규칙과 맞춰야 하는 경우

권장:

- `SignUpRequest`
- `MeetingListItem`
- `UserSummaryProjection`

비권장:

- `SignUpRequestDto`
- `MeetingListItemDto`
- `UserSummaryProjectionDto`

## 계층 간 변환 원칙

각 계층은 자신의 DTO만 직접 알아야 한다.

- Controller는 Controller DTO를 받는다.
- Controller는 Service DTO로 변환해서 Service에 전달한다.
- Service는 Service DTO를 반환한다.
- Controller는 필요한 경우 Service DTO를 Response로 변환한다.
- Repository projection은 API 응답으로 직접 노출하지 않는다.

## 회원가입 예시

회원가입 흐름은 아래처럼 가져간다.

```text
Client
  -> SignUpRequest
  -> AuthController
  -> SignUpCommand
  -> AuthService
  -> SignUpResult
  -> AuthController
  -> SignUpResponse
  -> Client
```

이 구조의 장점은 다음과 같다.

- Controller가 도메인 엔티티를 직접 알지 않는다.
- Service가 Controller 전용 DTO를 직접 알지 않는다.
- Request/Response와 유스케이스 입력/출력을 분리할 수 있다.
- Projection이 API 계약으로 새어 나가지 않는다.

## 금지 사항

- 모든 계층의 DTO를 `auth/dto` 같은 공용 패키지 하나에 계속 쌓지 않는다.
- Service가 `controller/response` DTO를 직접 반환하지 않는다.
- Repository projection을 Controller 응답으로 직접 반환하지 않는다.
- Entity를 Controller 응답 모델로 직접 사용하지 않는다.

## 적용 순서

리팩토링은 한 번에 전역 적용하지 않는다.

1. 현재 작업 중인 기능부터 적용한다.
2. Request/Response를 Controller 소유 패키지로 이동한다.
3. Service 입력/출력 모델을 `service/dto`로 분리한다.
4. 조회 최적화가 필요한 경우 projection을 `repository/projection`으로 분리한다.
5. 다음 기능으로 같은 규칙을 확장한다.

## 현재 기준

현재 Eventitta의 DTO 기준은 아래 문장으로 요약한다.

`DTO는 dto라는 이름보다 소유 계층과 사용 목적이 먼저 드러나야 한다.`
