# DTO 배치 및 네이밍 가이드

## 목적

이 문서는 Eventitta에서 DTO를 어디에 두고, 어떤 이름으로 작성할지에 대한 기준을 정리한다.

핵심 원칙은 두 가지다.

- DTO는 `dto` 폴더에 무분별하게 모으지 않는다.
- DTO는 "누가 만들고 누가 소비하는가"를 기준으로 배치한다.

계층 간 DTO 변환 구현은 기본적으로 `MapStruct`를 우선한다.

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
├── mapper
│   └── AuthMapper
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
- Controller는 Mapper를 통해 Service DTO로 변환해서 Service에 전달한다.
- Service는 Service DTO를 반환한다.
- Controller는 필요한 경우 Mapper를 통해 Service DTO를 Response로 변환한다.
- Repository projection은 API 응답으로 직접 노출하지 않는다.

## MapStruct 사용 원칙

계층 간 변환은 수동 생성보다 `MapStruct` 기반 mapper를 기본값으로 둔다.

- Mapper는 각 도메인 패키지 아래 `mapper` 패키지에 둔다.
- Mapper는 `@Mapper(componentModel = "spring")`를 사용한다.
- Controller와 Service는 mapper를 생성하지 말고 Spring 빈으로 주입받는다.
- Request -> Command
- Result -> Response
- Entity -> Response
- Projection -> Response
- 여러 입력을 조합한 응답 변환

위와 같은 단순 매핑은 DTO의 `static from`, `static of`, `toCommand()` 같은 수동 변환 메서드보다 mapper로 옮기는 것을 우선한다.

## MapStruct를 바로 쓰지 않는 경우

모든 변환을 기계적으로 mapper로 옮기지는 않는다.

- 비밀번호 인코딩처럼 외부 의존성이 직접 개입하는 경우
- 엔티티 생성 시 기본 role, provider, 상태값 같은 도메인 규칙을 강하게 보장해야 하는 경우
- 정렬, 집계, 랭킹 계산, 계층 경로 생성처럼 변환보다 계산이 본질인 경우
- 외부 API 호출, 캐시 탐색, DB 조회 조합이 핵심인 경우

이 경우에도 가능한 순수 필드 매핑 부분만 mapper로 분리하고, 비즈니스 규칙은 Service나 Domain에 남긴다.

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
- 매핑 규칙이 Mapper에 모여 중복 변환 코드가 줄어든다.

## 금지 사항

- 모든 계층의 DTO를 `auth/dto` 같은 공용 패키지 하나에 계속 쌓지 않는다.
- Service가 `controller/response` DTO를 직접 반환하지 않는다.
- Repository projection을 Controller 응답으로 직접 반환하지 않는다.
- Entity를 Controller 응답 모델로 직접 사용하지 않는다.
- Controller나 Service에 단순 DTO 조립 코드를 반복해서 작성하지 않는다.
- 단순 매핑을 위해 DTO에 `from`, `of`, `toCommand` 메서드를 계속 추가하지 않는다.

## 적용 순서

리팩토링은 한 번에 전역 적용하지 않는다.

1. 현재 작업 중인 기능부터 적용한다.
2. Request/Response를 Controller 소유 패키지로 이동한다.
3. Service 입력/출력 모델을 `service/dto`로 분리한다.
4. 계층 간 변환이 있다면 `mapper` 패키지에 MapStruct mapper를 추가하거나 확장한다.
5. 조회 최적화가 필요한 경우 projection을 `repository/projection`으로 분리한다.
6. 다음 기능으로 같은 규칙을 확장한다.

## 현재 기준

현재 Eventitta의 DTO 기준은 아래 문장으로 요약한다.

`DTO는 소유 계층과 사용 목적이 먼저 드러나야 하고, 계층 간 변환은 기본적으로 MapStruct mapper에 모은다.`
