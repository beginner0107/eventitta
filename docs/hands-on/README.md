# Eventitta 실습형 탐색 가이드

이 문서 묶음은 "앱을 로컬에서 띄운 뒤 실제 API를 호출하면서 모듈 구조를 이해하는" 용도로 작성되었습니다.

기존의 [ARCHITECTURE.md](../ARCHITECTURE.md), [MODULAR_MONOLITH_HANDOFF.md](../MODULAR_MONOLITH_HANDOFF.md), [TEST_GUIDELINES.md](../TEST_GUIDELINES.md)가 설계 원칙과 소유권을 설명한다면, 여기서는 실제 요청 흐름과 런타임 관찰 포인트를 중심으로 봅니다.

## 누가 읽으면 좋은가

- 멀티 모듈 리팩터 이후 구조를 다시 익혀야 하는 개발자
- 컨트롤러부터 서비스, 인프라 구현, SQL, Redis, 스케줄러까지 한 번에 따라가고 싶은 개발자
- Swagger만 보는 것이 아니라 "이 요청이 어느 모듈을 지나고 무엇을 건드리는지" 확인하고 싶은 개발자

## 선행 조건

1. 로컬 인프라 실행

```bash
cd infra
docker-compose up -d
```

2. 필수 환경 변수 설정

```bash
export MYSQL_PASSWORD=...
export SECRET_KEY=...
```

3. 애플리케이션 실행

```bash
./gradlew :eventitta-app:bootRun --args='--spring.profiles.active=local'
```

4. 기본 확인 포인트

- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- 로그 파일: `./logs/eventitta.log`, `./logs/error.log`

## 추천 읽기 순서

1. [01-module-and-request-flow.md](./01-module-and-request-flow.md)
2. [02-api-exploration.md](./02-api-exploration.md)
3. [03-scheduler-and-background-jobs.md](./03-scheduler-and-background-jobs.md)
4. [04-sql-cache-redis-observability.md](./04-sql-cache-redis-observability.md)

## 같이 열어둘 파일

- [../ARCHITECTURE.md](../ARCHITECTURE.md)
- [../TEST_GUIDELINES.md](../TEST_GUIDELINES.md)
- [../SCHEDULER_CONFIG_EXAMPLE.yml](../SCHEDULER_CONFIG_EXAMPLE.yml)

## 실행 자산

- [../http/auth-user-session.http](../http/auth-user-session.http)
  - 회원가입, 로그인, refresh, 로그아웃, 세션 조회/종료 흐름
- [../http/meeting-region-ranking-admin.http](../http/meeting-region-ranking-admin.http)
  - 지역 조회, 모임 생성/참가/승인, 랭킹 조회, 관리자 축제 sync 예시

## 실행하면서 확인할 것

- Swagger에서 현재 노출되는 엔드포인트와 문서가 일치하는지
- `eventitta.log`에서 `com.eventitta`, `p6spy`, `Scheduler` 로그가 어떻게 섞여 나오는지
- `/actuator/health`에서 MySQL/Redis 상태가 어떻게 보이는지
- `eventitta-app`, `eventitta-api`, `eventitta-domain`, `eventitta-infra` 중 어느 모듈에서 책임이 끝나는지

## 빠른 체크리스트

- 앱 실행 직후 `지역 캐시 워밍업` 로그가 찍히는지 확인한다.
- `POST /api/v1/auth/login` 후 `Set-Cookie`에 `access_token`, `refresh_token`이 내려오는지 본다.
- `POST /api/v1/auth/refresh` 후 `refresh_tokens` 관련 SQL이 어떻게 바뀌는지 본다.
- `GET /api/v1/regions/options`를 2번 호출해서 첫 호출과 이후 호출의 로그 차이를 본다.
- `GET /api/v1/rankings/top?type=POINTS` 호출 전후로 Redis 키를 확인한다.
- `shedlock` 테이블을 조회해서 실제 락 row가 어떤 이름으로 저장되는지 확인한다.
