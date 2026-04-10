# Performance Test Guide

이 저장소에는 지역/거리 조회 시나리오를 재현하기 위한 k6 기반 성능 테스트 스크립트가 포함돼 있다.

## 준비

```bash
brew install k6
docker --version
docker-compose --version
```

애플리케이션은 먼저 로컬에서 실행되어 있어야 한다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 모니터링 환경 시작

```bash
./performance-tests/run-test.sh start
```

- Grafana: `http://localhost:3000`
- InfluxDB: `http://localhost:8086`

## 테스트 실행

가장 단순한 방법은 래퍼 스크립트를 사용하는 것이다.

```bash
./performance-tests/run-test.sh test --base-url http://localhost:8080
```

직접 실행하려면:

```bash
k6 run \
  --out influxdb=http://localhost:8086/k6 \
  -e BASE_URL=http://localhost:8080 \
  performance-tests/region-baseline.js
```

## 시나리오

`performance-tests/region-baseline.js`는 아래 흐름을 반복한다.

- 지역 옵션 조회
- 하위 지역 조회
- 지역 계층 조회
- 중복 요청 시나리오

기본 threshold:

- `http_req_duration p95 < 500ms`
- `http_req_failed rate < 1%`
- `region_options_response_time p95 < 200ms`
- `region_hierarchy_response_time p95 < 100ms`

## 종료

```bash
./performance-tests/run-test.sh stop
```
