# Region API 성능 테스트

k6 + Grafana를 사용한 Region API 성능 테스트

## 빠른 시작

### 1. 한 번에 실행 (권장)

```bash
# 전체 실행 (환경 시작 + 테스트)
./performance-tests/run-test.sh all
```

### 2. 단계별 실행

```bash
# 1. 환경 시작
./performance-tests/run-test.sh start

# 2. 테스트 실행 (기본 BASE_URL: http://localhost:8080)
./performance-tests/run-test.sh test

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
- 환경변수(옵션보다 낮은 우선순위)
  - `BASE_URL` 또는 `TARGET_BASE_URL`
  - `PERF_INFLUX_URL`
  - `PERF_GRAFANA_URL`

예시

```bash
# 로컬이 아닌 DEV 환경 대상으로 실행
./performance-tests/run-test.sh test \
  --base-url https://dev.api.example.com \
  --influx-url http://localhost:8086/k6

# 다른 로컬 포트(8081)에서 앱이 뜬 경우
./performance-tests/run-test.sh test --base-url http://localhost:8081

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

## 캐싱 전후 비교

### Before (캐싱 전)

```bash
./performance-tests/run-test.sh test false
```

### After (캐싱 후)

```bash
# 1. 캐싱 코드 적용
# 2. 애플리케이션 재시작
./gradlew clean bootRun

# 3. 테스트 실행
./performance-tests/run-test.sh test true
```

## 파일 구조

```
performance-tests/
├── README.md                  # 이 파일
├── run-test.sh                # 실행 스크립트
└── region-baseline.js         # k6 테스트 시나리오
```

## 상세 문서

전체 가이드: [PERFORMANCE_TEST_GUIDE.md](../docs/PERFORMANCE_TEST_GUIDE.md)

## 요구사항

- k6 (`brew install k6`)
- Docker & Docker Compose
- Spring Boot 애플리케이션 실행 중

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
# 애플리케이션 실행
./gradlew bootRun

# (예시) BASE_URL이 달라진 경우 헬스 체크
curl "$BASE_URL/actuator/health"
# 기본값으로는
curl http://localhost:8080/actuator/health
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

### 캐싱 전

- p95 응답 시간: ~300ms
- 평균 응답 시간: ~150ms
- DB 쿼리: ~6,000회/테스트

### 캐싱 후 (목표)

- p95 응답 시간: ~30ms (10배 향상)
- 평균 응답 시간: ~15ms (10배 향상)
- DB 쿼리: ~60회/테스트 (99% 감소)
