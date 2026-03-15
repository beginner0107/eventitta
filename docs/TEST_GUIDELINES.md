# 테스트 코드 작성 가이드

## 목적

이 문서는 Eventitta에서 어떤 레이어를 어떤 방식으로 테스트할지에 대한 기본 원칙을 정리한다.

현재 기준으로 권장하는 기본 전략은 아래와 같다.

- Controller는 단위 테스트 또는 웹 슬라이스 테스트로 검증한다.
- Service는 통합 테스트를 기본 회귀망으로 삼는다.
- Repository는 모킹 기반 단위 테스트보다 `@DataJpaTest` 기반 슬라이스 테스트를 표준으로 삼는다.

현재 작성된 테스트 코드가 이 문서를 모두 반영하고 있지는 않다.
이 문서는 앞으로 테스트를 추가하거나 리팩토링할 때 따를 기준 문서다.

## 배경

이 문서는 아래 관점을 바탕으로 작성한다.

- 테스트의 목적은 커버리지 수치를 올리는 것이 아니라, 릴리즈해도 될 만큼의 확신을 주는 것이다.
- 테스트가 어려운 코드는 대개 의존성 방향이나 책임 분리가 좋지 않을 가능성이 높다.
- 프레임워크와 입출력은 테스트하기 어려운 험블 객체로 보고, 본질적인 정책과 유스케이스는 가능한 한 분리해서 검증한다.
- 테스트 데이터는 빌더나 팩토리 메서드로 읽기 쉽게 만들고, 필드 추가에 덜 취약하게 관리한다.
- 테스트 코드는 실행 가능한 문서여야 하며, 시나리오와 의도를 읽는 사람에게 전달해야 한다.

즉, 테스트 전략은 단순한 검증 도구 선택이 아니라 설계 원칙과 연결되어야 한다.

## Eventitta 기본 전략

### 1. Controller는 얇게 유지하고 단위 테스트한다

Controller는 HTTP 계약을 다루는 어댑터다.
요청 파싱, 검증, 상태 코드, 응답 직렬화, 예외 매핑을 검증하는 것이 주목적이다.

권장 방식:

- `@WebMvcTest` 기반으로 테스트한다.
- 공통 구성이 필요하면 [
  `ControllerTestSupport`](/Users/seungjooahn/dev/backend/eventitta/src/test/java/com/eventitta/ControllerTestSupport.java)
  를 사용한다.
- 비즈니스 로직은 Service mock에 위임하고, Controller 자체는 HTTP 계약만 검증한다.
- 인증/인가 필터 자체가 테스트 대상이 아니라면 `addFilters = false`를 유지한다.
- 인증/인가 시나리오가 핵심인 경우에만 필터 포함 테스트를 별도로 둔다.

Controller 테스트에서 검증할 것:

- URL, HTTP method, path variable, query parameter 바인딩
- request body 역직렬화
- `@Valid` 검증 결과
- 응답 status, header, body JSON 구조
- ControllerAdvice 또는 예외 응답 포맷
- 쿠키, 파일 업로드, 인증 principal 전달 같은 웹 계층 계약

Controller 테스트에서 검증하지 않을 것:

- Repository 호출 여부
- 트랜잭션 처리
- 복잡한 도메인 규칙
- JPA 매핑이나 Querydsl 쿼리 결과

### 2. Service는 통합 테스트를 기본으로 한다

Service는 유스케이스를 수행하는 중심 계층이다.
이 레이어에서 중요한 것은 "메서드 한 줄 한 줄"이 아니라 "유스케이스가 실제로 성립하는가"다.

권장 방식:

- 기본적으로 `@SpringBootTest` 기반 통합 테스트를 사용한다.
- 공통 설정이 필요하면 [
  `IntegrationTestSupport`](/Users/seungjooahn/dev/backend/eventitta/src/test/java/com/eventitta/IntegrationTestSupport.java)
  를 사용한다.
- DB, 트랜잭션, 엔티티 매핑, 스프링 빈 wiring은 실제 구성으로 검증한다.
- 외부 시스템만 mock 또는 fake로 대체한다.
- 다만 테스트 비용을 감안해, Service 유스케이스 검증에 필요한 범위만 실제로 띄운다.

Service 통합 테스트에서 검증할 것:

- 유스케이스 성공 흐름
- 예외 흐름과 롤백 여부
- 여러 Repository를 거치는 상태 변화
- 트랜잭션 경계에서의 정합성
- 이벤트 발행, 캐시 갱신, 후처리 같은 유스케이스 결과

Service 통합 테스트에서 mock으로 대체할 대상:

- 외부 HTTP API
- S3 같은 외부 스토리지
- Discord, 이메일, 메시지 브로커 같은 외부 알림 시스템
- 실제 Redis, 스케줄러 락, 외부 인증 시스템처럼 테스트 목적상 불필요한 외부 인프라

Service 통합 테스트에서 피할 것:

- 내부 협력 객체를 전부 mock으로 바꾸는 반쪽짜리 테스트
- 구현 상세에 과도하게 결합된 interaction 검증
- "이 메서드가 이 메서드를 불렀다" 수준의 검증만 하는 테스트

예외:

- 프레임워크와 거의 무관한 순수 계산 로직, 정책 객체, 도메인 규칙은 별도의 단위 테스트를 추가해도 된다.
- 이 경우에도 테스트 대상은 Service 전체보다 정책 객체나 도메인 객체로 더 작게 쪼개는 편이 낫다.

### 3. Repository는 `@DataJpaTest` 기반 슬라이스 테스트를 표준으로 한다

Repository는 스프링 데이터와 JPA에 강하게 의존한다.
그래서 Repository를 mock으로 흉내 내는 단위 테스트는 가치가 낮고, 오히려 실제 쿼리와 매핑을 놓치기 쉽다.

이 문서에서 Repository 테스트는 다음 의미로 사용한다.

- "pure unit test"가 아니라 "영속성 슬라이스 테스트"
- 즉, `@DataJpaTest`로 JPA와 Querydsl을 실제로 붙여 보는 테스트

권장 방식:

- `@DataJpaTest`를 사용한다.
- Querydsl custom repository면 필요한 설정만 `@Import`한다.
- 실제 엔티티를 저장하고 조회한다.
- 단순 CRUD보다 커스텀 쿼리, 정렬, 페이징, join, fetch 전략, projection 검증에 집중한다.

Repository 테스트를 써야 하는 경우:

- Querydsl 구현체가 있는 경우
- 복잡한 검색 조건이 있는 경우
- JPQL/native query를 직접 작성한 경우
- soft delete, paging, sorting, aggregation 같은 쿼리 규칙이 중요한 경우

Repository 테스트를 굳이 쓰지 않아도 되는 경우:

- 스프링 데이터가 자동 생성하는 매우 단순한 CRUD
- Service 통합 테스트가 이미 충분히 검증하는 단순 조회

현재 레포의 예시:

- [
  `PostRepositoryIntegrationTest`](/Users/seungjooahn/dev/backend/eventitta/src/test/java/com/eventitta/post/repository/PostRepositoryIntegrationTest.java)

이 예시는 이름은 `IntegrationTest`지만 역할상 Repository 슬라이스 테스트에 가깝다.

## 레이어별 표준 조합

### Controller

- 애노테이션: `@WebMvcTest`
- 기본 베이스: [
  `ControllerTestSupport`](/Users/seungjooahn/dev/backend/eventitta/src/test/java/com/eventitta/ControllerTestSupport.java)
- 대역: Service mock
- 핵심 검증: 요청/응답 계약, validation, 예외 응답
- 파일명: `*ControllerTest`

### Service

- 애노테이션: `@SpringBootTest`
- 기본 베이스: [
  `IntegrationTestSupport`](/Users/seungjooahn/dev/backend/eventitta/src/test/java/com/eventitta/IntegrationTestSupport.java)
- 대역: 외부 시스템만 mock 또는 fake
- 핵심 검증: 유스케이스 완결성, 상태 변화, 트랜잭션
- 파일명: `*ServiceIntegrationTest`

### Repository

- 애노테이션: `@DataJpaTest`
- 추가 설정: `QuerydslConfig` 등 최소 설정만 import
- 대역: 사용하지 않음
- 핵심 검증: 쿼리, 매핑, 페이징, 정렬, 조건 조합
- 파일명: `*RepositoryTest`

## 작성 규칙

### 1. 테스트는 행위와 결과를 설명해야 한다

- 테스트 하나는 가능하면 하나의 행위 또는 규칙을 설명해야 한다.
- `@DisplayName`은 비즈니스 문장으로 쓴다.
- 메서드 이름은 `when_then` 형태나 목적이 드러나는 이름을 사용한다.
- 테스트를 처음 보는 사람도 시나리오를 따라갈 수 있어야 한다.

좋은 예:

- `로그인에 성공하면 access token 과 refresh token 쿠키를 내려준다`
- `필터 없이 조회하면 모든 게시글이 페이징되어 반환된다`

### 2. 시나리오 단위로 작성한다

테스트는 함수 단위보다 시나리오 단위로 읽혀야 한다.
준비, 실행, 검증이 자연스럽게 보이도록 작성한다.

권장:

- `given`, `when`, `then` 주석 또는 구획 사용
- 시나리오에 필요한 준비만 노출
- 검증은 시나리오 결과 중심으로 작성

### 3. Happy path만 쓰지 않는다

기본적으로 아래 세 가지를 우선 본다.

- 정상 흐름
- 실패 흐름
- 경계 조건

특히 경계 조건은 빠지기 쉽다.
빈 값, null, 최소값, 최대값, 중복, 만료, 권한 없음 같은 케이스를 반드시 의식한다.

### 4. 구현 상세보다 비즈니스 결과를 검증한다

가능하면 아래를 검증한다.

- 반환값
- 저장된 상태
- 발행된 이벤트
- 생성된 응답
- 발생한 예외

아래는 최소화한다.

- 내부 private 메서드 호출 여부
- mock 객체 간 세세한 호출 순서
- 구현 변경에 쉽게 깨지는 interaction 중심 검증

### 5. Mock은 기본값이 아니라 도구다

stub과 mock을 구분해서 사용한다.

- stub은 미리 준비한 값을 돌려주는 용도로 사용한다.
- mock은 외부로 나가는 행위나 협력 계약을 검증할 때만 사용한다.

권장:

- Controller 테스트에서 Service를 stub 또는 mock 처리
- Service 통합 테스트에서 외부 알림, 외부 API, 스토리지 같은 outbound dependency만 mock 처리
- 상태 검증이 충분하면 interaction 검증을 생략

비권장:

- 내부 구현 흐름 전체를 mock 호출 횟수로 설명하는 테스트
- JPA Repository를 mock해서 쿼리 결과를 가정하는 Repository 테스트
- 하나의 테스트에서 stub, mock, spy를 과하게 섞는 방식

### 6. 테스트 데이터는 빌더 또는 팩토리로 감춘다

테스트 데이터 생성은 읽기 쉬워야 하고, 엔티티 필드가 늘어나도 테스트가 쉽게 깨지지 않아야 한다.

권장:

- 테스트 전용 빌더
- 도메인 팩토리 메서드
- fixture helper

비권장:

- 모든 테스트에서 엔티티 생성 코드를 반복 작성
- 의미 없는 문자열과 숫자를 하드코딩

### 7. 테스트는 서로 독립적이어야 한다

- 테스트 실행 순서에 의존하지 않는다.
- 다른 테스트가 남긴 데이터나 전역 상태를 기대하지 않는다.
- 시간 의존 로직은 `Clock` 같은 주입 가능한 방식으로 제어한다.
- 공유 fixture가 있다면 읽기 편의를 위한 공통화만 하고, 상태 공유는 피한다.

### 8. 테스트 비용을 관리한다

테스트도 비용이다.
그래서 항상 가장 넓은 테스트를 쓰는 것이 아니라, 목적에 맞는 가장 좁은 도구를 우선 선택한다.

- HTTP 계약 검증: `@WebMvcTest`
- JPA 쿼리 검증: `@DataJpaTest`
- 유스케이스 통합 검증: `@SpringBootTest`

즉, Service는 통합 테스트를 기본으로 하되, Controller와 Repository까지 전부 풀 컨텍스트로 검사하지는 않는다.

### 9. 테스트는 레이어 경계를 존중한다

- Controller 테스트에서 Repository를 직접 만지지 않는다.
- Service 통합 테스트에서 웹 요청 직렬화까지 같이 검증하지 않는다.
- Repository 테스트에서 비즈니스 유스케이스 전체를 검증하지 않는다.

각 테스트는 자신의 레이어 책임만 검증해야 한다.

## 권장 패턴

### Controller 테스트 예시

```java
class AuthControllerTest extends ControllerTestSupport {

  @Test
  @DisplayName("유효하지 않은 이메일이면 400을 반환한다")
  void login_invalidEmail_returnsBadRequest() throws Exception {
    SignInRequest request = new SignInRequest("invalid-email", "password123!");

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isBadRequest());
  }
}
```

포인트:

- HTTP 계약만 검증한다.
- Service 내부 로직은 여기서 검증하지 않는다.

### Service 통합 테스트 예시

```java

@Transactional
class RegionServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private RegionService regionService;

  @Autowired
  private RegionRepository regionRepository;

  @Test
  @DisplayName("활성 지역 목록을 조회하면 저장된 지역이 반환된다")
  void getRegions_returnsSavedRegions() {
    regionRepository.save(new Region("1100110100", "청운효자동", "1100100000", 3));

    List<RegionResponse> result = regionService.getRegions();

    assertThat(result).extracting(RegionResponse::name)
      .contains("청운효자동");
  }
}
```

포인트:

- 실제 빈과 실제 DB를 사용한다.
- 유스케이스 결과를 검증한다.

### Repository 테스트 예시

```java

@DataJpaTest
@Import(QuerydslConfig.class)
class PostRepositoryTest {

  @Autowired
  private PostRepository postRepository;

  @Test
  @DisplayName("제목으로 필터링하면 해당 키워드가 포함된 게시글만 조회된다")
  void findAllByFilter_filtersByTitle() {
    // given
    // when
    // then
  }
}
```

포인트:

- Querydsl, JPA 매핑, 실제 쿼리 결과를 검증한다.
- Repository를 mock으로 대체하지 않는다.

## 참고 자료

이 문서는 아래 강의 자료의 관점을 반영해 정리했다.

- `Java:Spring_강의`
- `Practical_Testing_강의`

반영한 핵심 포인트:

- 테스트의 목적은 커버리지가 아니라 배포에 대한 확신이다.
- 테스트는 문서이며, 시나리오 단위로 읽혀야 한다.
- 풍부한 단위 또는 슬라이스 테스트와 필요한 통합 테스트를 함께 가져간다.
- mock은 남용하지 말고, 상태 검증과 행위 검증을 구분해서 사용한다.
- 테스트 환경과 테스트 간 독립성을 보장해야 한다.

## 도입 순서

기존 테스트를 한 번에 전부 고치지 않는다.
다음 순서로 점진 적용한다.

1. 새로 작성하는 테스트부터 이 기준을 따른다.
2. Controller 테스트는 `@WebMvcTest` 중심으로 정리한다.
3. Service 테스트는 유스케이스 중심 통합 테스트를 우선 추가한다.
4. 복잡한 Repository 쿼리는 `@DataJpaTest`로 보강한다.
5. 반복되는 테스트 데이터 생성은 빌더 또는 fixture helper로 추출한다.

## 한 줄 기준

Eventitta의 테스트는 "레이어 책임에 맞는 가장 얇은 도구를 사용하되, 서비스 유스케이스에는 실제 실행에 가까운 확신을 준다"를 목표로 한다.
