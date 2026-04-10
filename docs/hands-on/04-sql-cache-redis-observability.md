# 4. SQL, 캐시, Redis 관찰 포인트

이 문서는 API를 실행하면서 MySQL, Flyway, Caffeine, Redis를 어디서 확인할지 정리합니다.

## 1. MySQL / Flyway / p6spy

### 먼저 볼 테이블

- `flyway_schema_history`
- `users`
- `refresh_tokens`
- `auth_identity`
- `auth_action_tokens`
- `auth_security_events`
- `meetings`
- `meeting_participants`
- `shedlock`

### 바로 실행할 수 있는 명령

현재 적용된 migration 확인:

```bash
MYSQL_PWD="$MYSQL_PASSWORD" mysql \
  -h 127.0.0.1 -P "${DB_PORT:-3306}" -u eventittaUser eventitta \
  -e "select installed_rank, version, description, success from flyway_schema_history order by installed_rank desc limit 10;"
```

인증/세션 관련 row 확인:

```bash
MYSQL_PWD="$MYSQL_PASSWORD" mysql \
  -h 127.0.0.1 -P "${DB_PORT:-3306}" -u eventittaUser eventitta \
  -e "select id, email, nickname, deleted, suspended, auth_version from users order by id desc limit 5; select id, user_id, session_id, token_key, expires_at, last_seen_at from refresh_tokens order by id desc limit 10;"
```

모임 관련 row 확인:

```bash
MYSQL_PWD="$MYSQL_PASSWORD" mysql \
  -h 127.0.0.1 -P "${DB_PORT:-3306}" -u eventittaUser eventitta \
  -e "select id, title, status, leader_id, start_time, end_time, current_members, max_members from meetings order by id desc limit 10; select id, meeting_id, user_id, status from meeting_participants order by id desc limit 10;"
```

ShedLock 확인:

```bash
MYSQL_PWD="$MYSQL_PASSWORD" mysql \
  -h 127.0.0.1 -P "${DB_PORT:-3306}" -u eventittaUser eventitta \
  -e "select name, lock_until, locked_at, locked_by from shedlock order by name;"
```

### 로그에서 볼 것

로컬 프로파일은 datasource가 `jdbc:p6spy:mysql://...` 라서 SQL이 로그에 드러납니다.

```bash
tail -f ./logs/eventitta.log | rg 'p6spy|org.hibernate.SQL|Flyway'
```

보면 좋은 패턴:

- 로그인 직후 `refresh_tokens` insert
- refresh 직후 `refresh_tokens` lock/read/delete/save
- 모임 생성 직후 `meetings` insert
- 참가/승인 직후 `meeting_participants` insert/update
- 앱 기동 직후 Flyway migration 로그

## 2. Caffeine 지역 캐시

현재 코드 기준 캐시 이름:

- `regions`
- `regionOptions`

관련 코드:

- `eventitta-domain/src/main/java/com/eventitta/domain/common/constants/CacheConstants.java`
- `eventitta-domain/src/main/java/com/eventitta/domain/region/service/RegionCacheService.java`
- `eventitta-domain/src/main/java/com/eventitta/domain/region/service/RegionService.java`

### 실제 캐시 entry

- `regions::allRegionsMap`
- `regionOptions::leafRegions`

### 관찰 포인트

- 앱 시작 직후 `RegionCacheService.warmUpCache()`가 실행된다.
- 첫 `GET /api/v1/regions/options` 호출 전후 로그를 보면 캐시 미스 여부를 확인할 수 있다.
- 지역 API는 DB를 직접 여러 번 치는 구조가 아니라 전체 region map을 캐시에 적재해 재사용한다.

### 로그로 확인

```bash
tail -f ./logs/eventitta.log | rg '지역 캐시|캐시 미스'
```

기대 로그:

- `지역 캐시 워밍업 시작...`
- `캐시 미스 - DB에서 전체 지역 데이터 로드`
- `지역 캐시 워밍업 완료`

## 3. Redis 랭킹

현재 코드 기준 랭킹 키:

- `ranking:points`
- `ranking:activity:count`

관련 코드:

- `eventitta-domain/src/main/java/com/eventitta/domain/gamification/domain/RankingType.java`
- `eventitta-api/src/main/java/com/eventitta/api/gamification/controller/RankingController.java`
- `eventitta-infra/src/main/java/com/eventitta/infra/gamification/service/RedisRankingService.java`

### 바로 실행할 수 있는 명령

키 확인:

```bash
redis-cli -h localhost -p 6379 KEYS 'ranking:*'
```

포인트 랭킹 top 10:

```bash
redis-cli -h localhost -p 6379 ZREVRANGE ranking:points 0 9 WITHSCORES
```

활동량 랭킹 top 10:

```bash
redis-cli -h localhost -p 6379 ZREVRANGE ranking:activity:count 0 9 WITHSCORES
```

### API와 연결해서 보는 방법

1. `GET /api/v1/rankings/top?type=POINTS&limit=10`
2. `GET /api/v1/rankings/me?type=POINTS`
3. `GET /api/v1/rankings/stats`

그 뒤 Redis 키를 보면 응답이 어떤 자료구조를 읽고 있는지 감이 빨라집니다.

## 4. Redis 알림 rate limit

현재 코드 기준 prefix:

- `ratelimit:alert:*`

관련 코드:

- `eventitta-infra/src/main/java/com/eventitta/infra/notification/service/ratelimit/RedisRateLimiter.java`
- `eventitta-app/src/test/java/com/eventitta/notification/service/ratelimit/RedisRateLimiterTest.java`

### 바로 실행할 수 있는 명령

```bash
redis-cli -h localhost -p 6379 KEYS 'ratelimit:alert:*'
```

특정 key TTL 확인:

```bash
redis-cli -h localhost -p 6379 TTL 'ratelimit:alert:TEST_ERROR:HIGH'
```

### 읽을 때 볼 포인트

- prefix는 `ratelimit:alert:`로 시작한다.
- Redis 장애 시 local Caffeine fallback으로 내려간다.
- 테스트는 Testcontainers Redis를 띄워 실제 분산 카운팅을 검증한다.

## 5. Redis 연결/헬스 체크

관련 코드:

- `eventitta-infra/src/main/java/com/eventitta/infra/common/config/redis/RedisConfig.java`
- `eventitta-infra/src/main/java/com/eventitta/infra/common/config/redis/RedisHealthConfig.java`

앱 시작 시 기대 로그:

- `Connecting to Redis at localhost:6379 (SSL: false, Auth: false)`

Health 확인:

```bash
curl http://localhost:8080/actuator/health
```

로컬 프로파일에서는 `management.endpoint.health.show-details=always` 이므로 Redis 관련 세부 정보가 같이 보일 수 있습니다.

## 6. 실전 관찰 루틴

가장 추천하는 순서는 아래입니다.

1. 앱 기동 로그에서 Flyway, Redis, 지역 캐시 워밍업을 먼저 본다.
2. `auth-user-session.http`를 실행하며 `users`, `refresh_tokens` 변화를 본다.
3. `meeting-region-ranking-admin.http`를 실행하며 `meetings`, `meeting_participants`와 Redis 키를 본다.
4. `shedlock` 테이블과 `/actuator/health`를 마지막으로 확인한다.

## 자주 보는 조합 명령

```bash
tail -f ./logs/eventitta.log | rg 'p6spy|Scheduler|Redis|지역 캐시|Flyway'
```

```bash
curl http://localhost:8080/actuator/health && echo
```

```bash
redis-cli -h localhost -p 6379 KEYS 'ranking:*' && redis-cli -h localhost -p 6379 KEYS 'ratelimit:alert:*'
```
