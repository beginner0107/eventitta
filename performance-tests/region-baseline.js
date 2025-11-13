// Region API 성능 테스트 - Baseline
// 실행: k6 run --out influxdb=http://localhost:8086/k6 performance-tests/region-baseline.js

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const errorRate = new Rate('errors');
const regionOptionsResponseTime = new Trend('region_options_response_time');
const regionHierarchyResponseTime = new Trend('region_hierarchy_response_time');
const dbQueryCounter = new Counter('db_query_calls');

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 50 },
    { duration: '3m', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '2m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    region_options_response_time: ['p(95)<200'],
    region_hierarchy_response_time: ['p(95)<100'],
  },
  tags: {
    test_type: 'baseline',
    cache_enabled: 'false',
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = '/api/v1/regions';

export default function () {
  group('Scenario 1: 게시글 작성 - 지역 선택', function () {
    const optionsRes = http.get(`${BASE_URL}${API_PREFIX}/options`, {
      tags: { name: 'getRegionOptions' },
    });

    check(optionsRes, {
      'getRegionOptions status is 200': (r) => r.status === 200,
      'getRegionOptions has data': (r) => JSON.parse(r.body).length > 0,
    }) || errorRate.add(1);

    regionOptionsResponseTime.add(optionsRes.timings.duration);
    dbQueryCounter.add(2);

    sleep(1);
    const seoulChildrenRes = http.get(`${BASE_URL}${API_PREFIX}/1100000000`, {
      tags: { name: 'getChildRegions_Seoul' },
    });

    check(seoulChildrenRes, {
      'getChildRegions status is 200': (r) => r.status === 200,
    });

    dbQueryCounter.add(1);

    sleep(0.5);
    const hierarchyRes = http.get(`${BASE_URL}${API_PREFIX}/1111000000/hierarchy`, {
      tags: { name: 'getRegionHierarchy' },
    });

    check(hierarchyRes, {
      'getRegionHierarchy status is 200': (r) => r.status === 200,
      'hierarchy has data': (r) => JSON.parse(r.body).length >= 1,
    }) || errorRate.add(1);

    regionHierarchyResponseTime.add(hierarchyRes.timings.duration);
    dbQueryCounter.add(1);

    sleep(2);
  });

  group('Scenario 2: 지역별 게시글 조회', function () {
    const regionCodes = [
      '1111000000',
      '1168000000',
      '2611000000',
      '2711000000',
      '2811000000',
    ];

    const randomCode = regionCodes[Math.floor(Math.random() * regionCodes.length)];

    const hierarchyRes = http.get(`${BASE_URL}${API_PREFIX}/${randomCode}/hierarchy`, {
      tags: { name: 'getRegionHierarchy_Random' },
    });

    check(hierarchyRes, {
      'random hierarchy status is 200': (r) => r.status === 200,
    });

    regionHierarchyResponseTime.add(hierarchyRes.timings.duration);
    dbQueryCounter.add(1);

    sleep(1);
  });

  group('Scenario 3: 중복 요청', function () {
    const res1 = http.get(`${BASE_URL}${API_PREFIX}/options`, {
      tags: { name: 'getRegionOptions_Duplicate1' },
    });

    sleep(0.1);

    const res2 = http.get(`${BASE_URL}${API_PREFIX}/options`, {
      tags: { name: 'getRegionOptions_Duplicate2' },
    });

    check(res2, {
      'duplicate request should be similar time': (r) => {
        const diff = Math.abs(res1.timings.duration - r.timings.duration);
        return diff < 50;
      },
    });

    dbQueryCounter.add(4);
  });
}

export function handleSummary(data) {
  console.log('\n========================================');
  console.log('    Region API 성능 테스트 - 베이스라인');
  console.log('========================================\n');

  console.log('[주요 메트릭]');
  console.log(`  - 총 요청 수: ${data.metrics.http_reqs.values.count}`);
  console.log(`  - 실패율: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`);
  console.log(`  - 평균 응답 시간: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms`);
  console.log(`  - p95 응답 시간: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
  if (data.metrics.http_req_duration.values['p(99)']) {
    console.log(`  - p99 응답 시간: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms`);
  }
  console.log('');

  if (data.metrics.region_options_response_time) {
    console.log('[getRegionOptions 성능]');
    console.log(`  - 평균: ${data.metrics.region_options_response_time.values.avg.toFixed(2)}ms`);
    console.log(`  - p95: ${data.metrics.region_options_response_time.values['p(95)'].toFixed(2)}ms\n`);
  }

  if (data.metrics.region_hierarchy_response_time) {
    console.log('[getRegionHierarchy 성능]');
    console.log(`  - 평균: ${data.metrics.region_hierarchy_response_time.values.avg.toFixed(2)}ms`);
    console.log(`  - p95: ${data.metrics.region_hierarchy_response_time.values['p(95)'].toFixed(2)}ms\n`);
  }

  if (data.metrics.db_query_calls) {
    console.log('[예상 DB 쿼리 수]');
    console.log(`  - 총 호출: ${data.metrics.db_query_calls.values.count}회`);
    console.log(`  - 초당 평균: ${data.metrics.db_query_calls.values.rate.toFixed(2)}회/s\n`);
  }

  console.log('[문제점]');
  console.log('  - 매 요청마다 DB 쿼리 실행');
  console.log('  - 중복 요청에도 캐싱 효과 없음');
  console.log('  - 동시 접속 증가 시 DB 부하 증가\n');

  console.log('[캐싱 적용 후 예상 효과]');
  console.log('  - DB 쿼리 99% 감소');
  console.log('  - 응답 시간 10배 이상 개선');
  console.log('  - 동시 접속 처리 능력 향상\n');

  return {
    'stdout': '',
    'summary.json': JSON.stringify(data, null, 2),
  };
}
