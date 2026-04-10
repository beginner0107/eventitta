# Eventitta 성능 테스트 가이드

이 문서는 Eventitta의 운영 리스크를 로컬 격리 환경에서 재현하기 위한 기준 절차를 정리합니다.

## 1. 격리 perf 스택 기동

```bash
docker compose -f infra/docker-compose.perf.yml up -d --build
```

기본 포트:

- 진입 Nginx: `http://localhost:18080`
- app-1 direct: `http://localhost:18081`
- app-2 direct: `http://localhost:18082`
- MySQL: `localhost:13306`
- Redis: `localhost:16379`
- Mock services: `http://localhost:18090`
- InfluxDB: `http://localhost:8086`
- Grafana: `http://localhost:3000`

구성:

- `perf` profile 앱 2개
- MySQL 8.0 / Redis 7
- Nginx load balancing
- 외부 축제/지오코딩 mock service
- InfluxDB + Grafana

`perf` profile은 운영 값과 유사한 DB pool, Redis timeout, async pool을 사용하고, 관측을 위해 `/actuator/metrics`를 노출합니다.

## 2. 시드 데이터 적재

```bash
./scripts/perf/load_seed_data.sh
```

기본 시드:

- users: `10,000`
- posts: `100,000`
- post_likes: `500,000`
- user_gamification_stats / user_activity_stats 포함

명시적으로 조절하려면:

```bash
./scripts/perf/load_seed_data.sh --users 2000 --posts 20000 --likes 100000
```

시드가 끝나면 앱 replica를 자동 재기동합니다. 이유는 랭킹 warm-up이 앱 시작 시점에만 실행되기 때문입니다.

기본 관리자 계정:

- email: `perfadmin@example.com`
- password: `Pass123!`

## 3. 부하 시나리오 실행

### 게시글 조회 read path

```bash
./performance-tests/run-test.sh test \
  --scenario posts-read \
  --base-url http://localhost:18080
```

측정 포인트:

- `posts_list_duration`
- `posts_search_duration`
- `post_detail_duration`
- DB slow query / Hikari active-pending

### 게시글 hotspot write path

```bash
./performance-tests/run-test.sh test \
  --scenario posts-write \
  --base-url http://localhost:18080
```

핫스팟 대상 게시글을 바꾸려면:

```bash
HOT_POST_IDS=2000000,2000001,2000002 \
./performance-tests/run-test.sh test --scenario posts-write --base-url http://localhost:18080
```

### 업로드/다운로드 path

기본 fixture는 작은 PNG 하나만 포함합니다. 현실적인 메모리/GC 압박을 보려면 3MB~8MB 이미지 여러 장을 직접 지정하세요.

```bash
UPLOAD_FIXTURE_PATHS=/absolute/path/a.png,/absolute/path/b.png \
./performance-tests/run-test.sh test \
  --scenario media-upload \
  --base-url http://localhost:18080
```

측정 포인트:

- `media_upload_duration`
- `media_download_duration`
- app heap / GC / Media executor saturation

### 게이미피케이션 projection pressure

```bash
./performance-tests/run-test.sh test \
  --scenario gamification \
  --base-url http://localhost:18080
```

시나리오 자체는 게시글/댓글 생성과 ranking 조회를 섞어 async projection 경로를 압박합니다.

부하 후 정합성 확인:

```bash
./scripts/perf/check_gamification_consistency.sh --base-url http://localhost:18080
```

## 4. 외부 연동 latency / error 주입

mock service는 환경 변수로 응답 지연과 상태 코드를 조절합니다.

예시: 전국 축제 API를 2초 지연으로 재기동

```bash
MOCK_NATIONAL_DELAY_MS=2000 \
docker compose -f infra/docker-compose.perf.yml up -d mock-services
```

예시: 서울 축제 API를 500으로 재기동

```bash
MOCK_SEOUL_STATUS=500 \
docker compose -f infra/docker-compose.perf.yml up -d mock-services
```

예시: 지오코딩을 10초 지연으로 재기동

```bash
MOCK_GEOCODING_DELAY_MS=10000 \
docker compose -f infra/docker-compose.perf.yml up -d mock-services
```

이후 확인 대상:

- 스케줄러 로그 장기 점유
- 외부 API 실패 시 복구 여부
- 재시도 부재에 따른 작업 시간 증가

## 5. Scale-out 검증

멀티 인스턴스 warm-up 및 projection을 보기 위한 최소 절차:

```bash
docker compose -f infra/docker-compose.perf.yml restart app-1 app-2
curl "http://localhost:18080/api/v1/rankings/top?type=POINTS&limit=10"
curl "http://localhost:18080/api/v1/rankings/stats"
```

앱 direct 포트를 같이 보면 인스턴스별 상태 비교가 쉽습니다.

```bash
curl http://localhost:18081/actuator/metrics
curl http://localhost:18082/actuator/metrics
```

## 6. 통과 기준

- 게시글 조회: `error rate < 1%`, `p95 < 300~500ms`, pool 고갈 없음
- 업로드: OOM 없음, 긴 Full GC 없음, queue saturation 시 요청 지연 급등 여부 확인
- 게이미피케이션: aggregate stats와 ranking stats 불일치가 없어야 함
- 외부 연동: delay/error 주입 시 전체 앱이 장시간 끌려가지 않아야 함

## 7. 정리 포인트

면접에서 핵심은 "무작정 최적화가 아니라, 코드 구조상 위험한 read path / async path / media path / external path를 먼저 가설로 세우고, prod-like 격리 환경에서 재현했다"는 순서입니다.
