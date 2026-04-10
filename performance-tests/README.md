# Eventitta 성능 테스트

k6 + Grafana를 사용한 Eventitta 성능/부하 테스트 모음입니다.

기본 제공 시나리오:

- `region`: 기존 Region API baseline
- `posts-read`: 게시글 목록/검색/상세 read path
- `posts-write`: 좋아요/취소/댓글 생성 hotspot write path
- `media-upload`: 업로드/다운로드 및 미디어 후처리 path
- `gamification`: 게시글/댓글 생성 후 ranking projection pressure

## 빠른 시작

### 1. 한 번에 실행 (권장)

```bash
# 전체 실행 (모니터링 환경 시작 + 기본 region 테스트)
./performance-tests/run-test.sh all
```

### 2. 단계별 실행

```bash
# 1. 환경 시작
./performance-tests/run-test.sh start

# 2. 테스트 실행 (기본 BASE_URL: http://localhost:8080, 기본 시나리오: region)
./performance-tests/run-test.sh test

# 게시글 조회 부하
./performance-tests/run-test.sh test --scenario posts-read --base-url http://localhost:18080

# 업로드 부하
UPLOAD_FIXTURE_PATHS=./performance-tests/fixtures/sample-upload.png \
./performance-tests/run-test.sh test --scenario media-upload --base-url http://localhost:18080

# 3. Grafana에서 결과 확인 (기본)
# http://localhost:3000 (admin/admin)

# 4. 환경 종료
./performance-tests/run-test.sh stop
```

## 환경별 설정(포트/호스트 하드코딩 제거)

다음 옵션 또는 환경변수로 대상/모니터링 URL을 설정할 수 있습니다. 옵션 > 환경변수 > 기본값 순으로 적용됩니다.

- 옵션
  - `--base-url <url>`: 대상 애플리케이션 BASE URL (기본: http://localhost:8080)
  - `--influx-url <url>`: InfluxDB 수집 URL (기본: http://localhost:8086/k6)
  - `--grafana-url <url>`: Grafana 접속 URL (기본: http://localhost:3000)
  - `--scenario <name>`: 실행할 시나리오 (`region`, `posts-read`, `posts-write`, `media-upload`, `gamification`)
- 환경변수(옵션보다 낮은 우선순위)
  - `BASE_URL` 또는 `TARGET_BASE_URL`
  - `PERF_INFLUX_URL`
  - `PERF_GRAFANA_URL`

예시

```bash
# 로컬이 아닌 DEV/perf 환경 대상으로 실행
./performance-tests/run-test.sh test \
  --scenario posts-read \
  --base-url http://localhost:18080 \
  --influx-url http://localhost:8086/k6

# 쓰기 hotspot 테스트
./performance-tests/run-test.sh test --scenario posts-write --base-url http://localhost:18080

# 다른 로컬 포트(8081)에서 앱이 뜬 경우
./performance-tests/run-test.sh test --scenario region --base-url http://localhost:8081

# 환경변수로 제어(옵션 생략 가능)
export BASE_URL=https://staging.api.example.com
export PERF_INFLUX_URL=http://localhost:8086/k6
export PERF_GRAFANA_URL=http://localhost:3000
./performance-tests/run-test.sh all
```

k6 스크립트는 BASE_URL을 환경변수로 전달받습니다. run-test.sh가 자동으로 `-e BASE_URL=...`을 주입합니다.

## Grafana 접속

- 기본 URL: http://localhost:3000 (구성 가능: `--grafana-url` 또는 `PERF_GRAFANA_URL`)
- ID/PW: admin / admin
- 대시보드: "Region API Performance Test"

## 격리 perf 환경

운영과 유사한 `2-replica + nginx + mysql + redis + mock-services + influxdb + grafana` 스택은 아래 compose 파일로 띄웁니다.

```bash
docker compose -f infra/docker-compose.perf.yml up -d --build

# 시드 데이터 적재 후 app replica 재기동
./scripts/perf/load_seed_data.sh

# 이후 테스트 실행
./performance-tests/run-test.sh test --scenario posts-read --base-url http://localhost:18080
```

격리 환경 가이드는 [docs/PERFORMANCE_TEST_GUIDE.md](../docs/PERFORMANCE_TEST_GUIDE.md)를 참고하세요.

## 기존 Region 캐싱 비교

```bash
# 캐싱 전
./performance-tests/run-test.sh test false --scenario region

# 캐싱 후
./performance-tests/run-test.sh test true
```

## 파일 구조

```
performance-tests/
├── README.md                  # 이 파일
├── fixtures/                  # 업로드 테스트용 샘플 파일
├── lib/                       # k6 공용 유틸
├── posts-read.js              # 게시글 read 부하
├── posts-write-hotspot.js     # 게시글 write hotspot 부하
├── media-upload.js            # 업로드/다운로드 부하
├── gamification-projection.js # 비동기 projection 부하
├── run-test.sh                # 실행 스크립트
└── region-baseline.js         # k6 테스트 시나리오
```

## 상세 문서

전체 가이드: [PERFORMANCE_TEST_GUIDE.md](../docs/PERFORMANCE_TEST_GUIDE.md)

## 요구사항

- k6 (`brew install k6`)
- Docker & Docker Compose
- 대상 애플리케이션 실행 중
- 업로드 부하의 경우 적절한 fixture 파일

## 문제 해결

### "k6: command not found"

```bash
# macOS
brew install k6

# 확인
k6 version
```

### "애플리케이션이 실행되지 않음"

```bash
# 로컬 앱 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# perf stack 실행
docker compose -f infra/docker-compose.perf.yml up -d --build
./scripts/perf/load_seed_data.sh

# BASE_URL이 달라진 경우 헬스 체크
curl "$BASE_URL/actuator/health"
```

### Grafana에 데이터가 안 보임

```bash
# InfluxDB 재시작
cd infra
docker-compose -f docker-compose.performance.yml restart influxdb

# 데이터 확인
docker exec -it eventitta-influxdb influx
> USE k6
> SHOW MEASUREMENTS
```

## 예상 결과

### 게시글 조회

- 에러율: `< 1%`
- p95: `300~500ms` 이내
- DB pool 고갈 없음

### 업로드

- OOM 없음
- 긴 Full GC 없음
- 큐 포화 시 요청 시간이 급격히 튀는지 확인

### 게이미피케이션

- 부하 후 `./scripts/perf/check_gamification_consistency.sh` 결과가 일치해야 함
