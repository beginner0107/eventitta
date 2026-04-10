<div align="center">
  <img src="docs/images/eventitta-logo.png" alt="Eventitta" width="180"/>
  <h1>Eventitta</h1>
  <p><strong>지역 기반 커뮤니티 플랫폼을 모듈형 모놀리스로 재구성한 Spring Boot 백엔드</strong></p>
</div>

## Overview

Eventitta는 지역 정보를 기반으로 게시글, 댓글, 모임, 이미지 업로드, 활동 랭킹을 제공하는 커뮤니티 서비스다.  
이 저장소에서는 단일 Spring Boot 백엔드를 유지하면서도 `app / api / domain / infra` 경계를 강제하는 modular monolith 구조로 리팩터링했다.

| 항목 | 내용 |
| --- | --- |
| 언어 / 프레임워크 | Java 17, Spring Boot 3.4.5 |
| 데이터 계층 | Spring Data JPA, QueryDSL, MySQL, Redis |
| 인프라 | Docker, AWS RDS/S3, GitHub Actions, Nginx |
| 테스트 | `./gradlew test` |

## What I Focused On

- 기능을 나누는 수준이 아니라 의존 방향을 강제하는 모듈 구조를 만들었다.
- 모임 승인처럼 정합성이 중요한 흐름에는 비관적 락을, 포인트 증감처럼 단순 연산에는 원자적 SQL 업데이트를 적용했다.
- 지역/거리 조회 성능을 개선하고, k6 기반 재현 스크립트와 문서로 결과를 남겼다.

## Modular Monolith

```text
eventitta-app     : 실행 진입점, 조립, ArchUnit 규칙
eventitta-api     : HTTP controller, security/web adapter, request/response mapper
eventitta-domain  : 도메인 규칙, use case service, repository contract, internal facade
eventitta-infra   : JPA repository, Redis/S3 adapter, scheduler, 외부 연동 구현
```

이 구조에서 도메인 간 직접 참조는 최소화하고, 필요한 상호작용은 internal facade를 통해 넘긴다.  
세부 설계는 [ARCHITECTURE.md](docs/ARCHITECTURE.md)에서 설명한다.

## Highlights

| 주제 | 결과 |
| --- | --- |
| 모듈 경계 정리 | 기능 슬라이스를 stacked PR로 분해하고 `./gradlew test` 기준으로 안정화 |
| 동시성 제어 | 모임 승인 정원 초과 문제를 비관적 락으로 차단 |
| 조회 성능 | 지역/거리 검색 쿼리 구조를 바꿔 응답 시간을 40~65% 개선 |

문제 정의, 선택지 비교, 구현 이유는 [TECHNICAL_CHALLENGES.md](docs/TECHNICAL_CHALLENGES.md)에 정리했다.  
성능 재현 방법은 [PERFORMANCE_TEST_GUIDE.md](docs/PERFORMANCE_TEST_GUIDE.md)에서 확인할 수 있다.

## Architecture Snapshot

![Architecture](docs/images/eventitta-architecture.png)

## Run

```bash
./gradlew test
./gradlew bootRun --args='--spring.profiles.active=local'
```

Swagger UI는 애플리케이션 실행 후 `http://localhost:8080/swagger-ui.html`에서 확인할 수 있다.
