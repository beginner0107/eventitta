# Eventitta 성능 테스트 결과

작성일: 2026-03-27  
실행 환경: macOS (Apple Silicon, M4 Pro, 24GB RAM)  
대상 환경: `perf` 격리 스택 (`app-1`, `app-2`, `nginx`, `mysql`, `redis`, `influxdb`, `grafana`)

## 1. 테스트 목적

실트래픽이 없는 상태에서 무작정 최적화하지 않고, 운영 리스크가 높은 경로를 가설 기반으로 검증하는 것이 목적이다.

이번 라운드에서는 아래 3개 시나리오를 실행했다.

- `posts-read`: 게시글 목록 / 검색 / 상세 조회 read path 검증
- `posts-write`: 좋아요 / 좋아요 취소 / 댓글 생성 hotspot write path 검증
- `gamification`: async projection 및 랭킹 조회 정합성 검증

## 2. 공통 조건

### 데이터 시드

아래 데이터 규모로 테스트를 진행했다.

```bash
./scripts/perf/load_seed_data.sh --users 2000 --posts 20000 --likes 100000
```

- users: `2,000`
- posts: `20,000`
- post_likes: `100,000`

### 대상 URL

```text
http://localhost:18080
```

Nginx를 통해 2개 app replica로 분산되는 구조에서 측정했다.

## 3. 사전 정비 사항

부하 테스트 시작 전에 perf 환경 자체를 먼저 안정화했다. 테스트 실패와 애플리케이션 버그를 구분하기 위해 필요한 수정이었다.

- `JWT_SECRET` 기본값이 너무 짧아 앱이 기동 직후 실패하던 문제 수정
- `V10__Add_evaluation_type_to_badge_rules.sql`의 MySQL 비호환 문법 수정
- `perf` 프로필에서 Spring Security filter chain 충돌 수정
- `auth_security_events` 테이블에 `updated_at` 컬럼이 없어 회원가입이 500으로 실패하던 문제 수정
- k6 인증 helper를 `회원가입 후 즉시 로그인` 방식에서 `시드된 인증 완료 유저 로그인` 방식으로 변경
- 시드 적재 스크립트가 앱 재기동 후 health check 완료까지 기다리도록 보강

즉, 이번 결과는 “테스트 스크립트가 잘못되어 나온 값”이 아니라, perf 환경을 정리한 뒤 측정한 결과다.

## 4. 시나리오 1: Posts Read

### 실행 명령

```bash
./performance-tests/run-test.sh test --scenario posts-read --base-url http://localhost:18080
```

### 부하 조건

- 최대 `100 VUs`
- 총 `4분` 부하
- 단계: `20 -> 50 -> 100 -> 0`

### k6 결과

```text
[Requests] 24253
[Errors] 0.00%
[Avg] 533.02ms
[p95] 1889.54ms
[posts_list_duration] avg=399.48ms p95=1295.79ms
[posts_search_duration] avg=1267.87ms p95=2608.62ms
[post_detail_duration] avg=281.87ms p95=995.19ms
```

### 임계치 결과

k6 threshold 초과로 실패 처리되었다.

- `http_req_duration`
- `posts_list_duration`
- `posts_search_duration`
- `post_detail_duration`

### 해석

- 장애는 아니었다. `Errors 0.00%`로 기능 실패는 없었다.
- 하지만 응답시간 목표는 명확히 초과했다.
- 전체 `p95`가 `1.89s`까지 상승했다.
- 특히 검색 경로가 가장 심각했다.
  - `posts_search_duration p95 = 2608.62ms`
- 목록 조회도 병목이 존재한다.
  - `posts_list_duration p95 = 1295.79ms`
- 상세 조회는 상대적으로 덜 느리지만, 여전히 `1초` 수준까지 상승한다.
  - `post_detail_duration p95 = 995.19ms`

### 결론

현재 프로젝트의 우선 병목 후보는 write path가 아니라 read/search path다.  
특히 게시글 검색과 목록 조회 쿼리가 가장 먼저 확인해야 할 영역이다.

## 5. 시나리오 2: Posts Write Hotspot

### 실행 명령

```bash
./performance-tests/run-test.sh test --scenario posts-write --base-url http://localhost:18080
```

### 부하 조건

- 최대 `30 VUs`
- 총 `4분` 부하
- 대상 액션:
  - 인기 게시글 상세 조회
  - 좋아요
  - 좋아요 취소
  - 댓글 생성

### k6 결과

```text
[Requests] 137585
[Errors] 0.20%
[Avg] 32.05ms
[p95] 104.06ms
[hot_post_read_duration] avg=31.33ms p95=103.52ms
[post_like_duration] avg=29.81ms p95=103.28ms
[comment_create_duration] avg=38.66ms p95=108.53ms
```

### 임계치 결과

k6 threshold 기준에서는 통과했다.

### 해석

- write path는 read path에 비해 매우 안정적이었다.
- 좋아요 / 좋아요 취소 / 댓글 생성 모두 `p95 100ms` 수준이었다.
- 동일 게시글에 트래픽이 몰리는 hotspot 조건에서도 큰 lock contention은 보이지 않았다.
- `Errors 0.20%`는 남았지만 전체적으로는 낮은 편이다.
  - 비즈니스 충돌성 응답(예: 이미 좋아요 상태, 이미 취소 상태)인지 로그로 구분 확인이 필요하다.
  - 현 시점에서는 전체 write path 품질을 뒤집을 수준의 수치는 아니다.

### 결론

현재 write hotspot 경로는 상대적으로 안정적이다.  
즉, 이 프로젝트의 핵심 운영 리스크는 “쓰기 경합”보다 “조회/검색 성능 저하”에 가깝다.

## 6. 시나리오 3: Gamification Projection Burst

### 실행 명령

```bash
./performance-tests/run-test.sh test --scenario gamification --base-url http://localhost:18080
./scripts/perf/check_gamification_consistency.sh --base-url http://localhost:18080
```

### 부하 조건

- 최대 `15 VUs`
- 총 `4분` 부하
- 대상 액션:
  - 게시글 생성
  - 댓글 생성
  - 개인 랭킹 조회
  - 랭킹 통계 조회

### 최초 실패 원인

최초 실행에서는 `Errors 33.32%`로 실패했다. 하지만 이 값은 실제 gamification projection 붕괴가 아니라 테스트 시나리오의 검증 요청 설정 오류였다.

- `/api/v1/rankings/stats`는 인증이 필요한 API인데, 초기 k6 스크립트가 이를 무인증으로 호출하고 있었다.
- 따라서 매 iteration마다 랭킹 통계 조회가 `401`로 실패했고, 그 결과 전체 실패율이 약 `33%`로 부풀려졌다.

이후 k6 시나리오와 consistency check 스크립트를 모두 인증 포함 방식으로 수정한 뒤 전체 테스트를 다시 수행했다.

### 수정 후 k6 결과

```text
[Requests] 29145
[Errors] 0.00%
[Avg] 7.87ms
[p95] 14.32ms
[gamification_action_duration] avg=8.87ms p95=12.12ms
[ranking_query_duration] avg=7.28ms p95=15.05ms
```

### 정합성 검증 결과

```text
[consistency] db.points=2000 api.points=2000
[consistency] db.activity=2000 api.activity=2000
[consistency] ranking projection matches aggregate stats
```

### 임계치 결과

수정 후 재실행 기준으로 k6 threshold는 통과했다.

### 해석

- 현재 부하 조건에서는 gamification action 자체의 처리 시간은 매우 짧았다.
- 랭킹 조회 역시 `p95 15ms` 수준으로 안정적이었다.
- 부하 후 DB 집계값과 랭킹 API 통계값도 일치했다.
- 즉, 이번 규모의 burst에서는 “projection 누락”이나 “랭킹 통계 불일치”는 재현되지 않았다.

### 결론

이번 결과만 놓고 보면 gamification async projection은 현재 부하 범위에서는 안정적이다.  
다만 이 영역은 executor saturation 시 누락 가능성이 설계상 남아 있으므로, 더 높은 burst나 queue saturation 로그 기반 검증은 후속 과제로 남는다.

## 7. 종합 판단

이번 3개 시나리오 기준으로 정리하면 아래와 같다.

- `posts-read`: 실패
- `posts-write`: 통과
- `gamification`: 통과

즉, 현재 시스템은 “쓰기 병목”이나 “현재 수준의 projection 정합성 문제”보다 “조회 병목”이 더 크다.

면접에서는 아래처럼 정리할 수 있다.

> 로컬 prod-like 격리 환경에서 부하를 재현한 결과, 게시글 write hotspot과 gamification projection 경로는 현재 부하 수준에서 안정적이었지만, 게시글 read path는 100 VU에서 전체 p95가 1.89초까지 상승했고 검색은 p95 2.6초를 넘겼다. 따라서 현재 프로젝트의 핵심 운영 리스크는 락 경쟁보다 목록/검색 쿼리 성능 저하라고 판단했다.

## 8. 다음 액션

다음 검증 우선순위는 아래 순서가 적절하다.

1. 게시글 검색 쿼리 실행 계획 확인
2. 게시글 목록 조회 쿼리 실행 계획 확인
3. 더 높은 burst 또는 executor saturation 관측 조건에서 `gamification` 재검증
4. `media-upload` 시나리오로 업로드/메모리 리스크 검증

## 9. 참고 명령

### 앱/인프라 상태 확인

```bash
docker compose -f infra/docker-compose.perf.yml ps
curl http://localhost:18080/actuator/health
```

### 앱 로그 확인

```bash
docker compose -f infra/docker-compose.perf.yml logs -f app-1 app-2
```

### 리소스 확인

```bash
docker stats
```

### Grafana

```text
http://localhost:3000
```
