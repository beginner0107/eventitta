# ShedLock 시간 포맷 ISO-8601 통일

## 수정 개요

ShedLock의 `lockAtMostFor`와 `lockAtLeastFor` 파라미터에 사용되는 시간 포맷을 **ISO-8601 Duration 표준**으로 통일했습니다.

**작업 범위**: 2개 파일, 4개 스케줄러 메서드
**수정 유형**: 시간 포맷 표준화 (non-ISO → ISO-8601)
**영향도**: 기능 변경 없음 (표기법만 변경)

---

## 수정 이유

### 1. 국제 표준 준수

**ISO-8601 Duration Format**은 날짜/시간 표현의 국제 표준입니다:
- **PT**: Period Time (시간 구간을 의미)
- **H**: Hours (시간)
- **M**: Minutes (분, PT 뒤에 사용 시)
- **S**: Seconds (초)

```
예시:
- PT1H    = 1시간
- PT5M    = 5분
- PT30S   = 30초
- PT2H30M = 2시간 30분
```

### 2. ShedLock 공식 권장 포맷

ShedLock 공식 문서는 **ISO-8601 Duration 포맷 사용을 권장**합니다:
- ✅ 권장: `lockAtMostFor = "PT30M"` (ISO-8601)
- ⚠️  허용: `lockAtMostFor = "30m"` (간편 표기)

간편 표기법도 동작하지만, **공식 권장 사항은 ISO-8601 표준 사용**입니다.

### 3. 코드 일관성 확보

프로젝트 내 7개 스케줄러 중:
- **5개 스케줄러**: 이미 ISO-8601 사용 중 ✅
- **2개 스케줄러**: non-ISO 포맷 사용 ❌

→ 소수의 예외를 다수의 표준에 맞춰 **일관성 확보**

### 4. 가독성 및 명확성

**ISO-8601 포맷의 장점**:
- `"PT30S"` → "Period Time 30 Seconds" (명확한 의미)
- `"PT1H"` → "Period Time 1 Hour" (시간 단위 명확)
- 국제 표준이므로 **다국적 팀에서도 즉시 이해 가능**

**간편 표기법의 단점**:
- `"30s"`, `"1h"` → 어떤 프레임워크의 표기법인지 불명확
- ISO-8601 표준과 혼용 시 일관성 저하

### 5. 향후 확장성

ISO-8601은 복잡한 시간 표현도 지원합니다:
```java
// ISO-8601은 복합 표현 가능
lockAtMostFor = "PT1H30M"  // 1시간 30분

// 간편 표기법은 불가능
lockAtMostFor = "1h30m"    // ❌ 파싱 실패 가능성
```

---

## 수정 내용

### 1. FailedActivityEventRetryScheduler

**파일 경로**: `src/main/java/com/eventitta/gamification/scheduler/FailedActivityEventRetryScheduler.java`

**수정 전** (Line 33):
```java
@SchedulerLock(name = "retryFailedActivityEvents", lockAtMostFor = "55s", lockAtLeastFor = "5s")
```

**수정 후** (Line 33):
```java
@SchedulerLock(name = "retryFailedActivityEvents", lockAtMostFor = "PT55S", lockAtLeastFor = "PT5S")
```

**변환 내용**:
- `"55s"` → `"PT55S"` (55초)
- `"5s"` → `"PT5S"` (5초)

---

### 2. RankingScheduler - performFullSync()

**파일 경로**: `src/main/java/com/eventitta/gamification/scheduler/RankingScheduler.java`

**수정 전** (Line 46-49):
```java
@SchedulerLock(
    name = "RankingScheduler_fullSync",
    lockAtMostFor = "1h",
    lockAtLeastFor = "5m"
)
```

**수정 후** (Line 46-50):
```java
@SchedulerLock(
    name = "RankingScheduler_fullSync",
    lockAtMostFor = "PT1H",
    lockAtLeastFor = "PT5M"
)
```

**변환 내용**:
- `"1h"` → `"PT1H"` (1시간)
- `"5m"` → `"PT5M"` (5분)

---

### 3. RankingScheduler - performIncrementalSync()

**수정 전** (Line 71-74):
```java
@SchedulerLock(
    name = "RankingScheduler_incrementalSync",
    lockAtMostFor = "10m",
    lockAtLeastFor = "1m"
)
```

**수정 후** (Line 71-75):
```java
@SchedulerLock(
    name = "RankingScheduler_incrementalSync",
    lockAtMostFor = "PT10M",
    lockAtLeastFor = "PT1M"
)
```

**변환 내용**:
- `"10m"` → `"PT10M"` (10분)
- `"1m"` → `"PT1M"` (1분)

---

### 4. RankingScheduler - performWeeklyRebuild()

**수정 전** (Line 91-94):
```java
@SchedulerLock(
    name = "RankingScheduler_weeklyRebuild",
    lockAtMostFor = "2h",
    lockAtLeastFor = "10m"
)
```

**수정 후** (Line 91-95):
```java
@SchedulerLock(
    name = "RankingScheduler_weeklyRebuild",
    lockAtMostFor = "PT2H",
    lockAtLeastFor = "PT10M"
)
```

**변환 내용**:
- `"2h"` → `"PT2H"` (2시간)
- `"10m"` → `"PT10M"` (10분)

---

## 변환 규칙 요약

| 간편 표기 | ISO-8601 | 설명 |
|----------|----------|------|
| `"5s"`   | `"PT5S"` | 5초 |
| `"55s"`  | `"PT55S"` | 55초 |
| `"1m"`   | `"PT1M"` | 1분 |
| `"5m"`   | `"PT5M"` | 5분 |
| `"10m"`  | `"PT10M"` | 10분 |
| `"1h"`   | `"PT1H"` | 1시간 |
| `"2h"`   | `"PT2H"` | 2시간 |

**변환 패턴**:
- 초(s) → PT{숫자}S
- 분(m) → PT{숫자}M
- 시(h) → PT{숫자}H

---

## 전체 스케줄러 현황 (수정 후)

| 스케줄러 | 메서드 | lockAtMostFor | lockAtLeastFor | 포맷 |
|---------|--------|---------------|----------------|------|
| **FailedActivityEventRetryScheduler** | retryFailedEvents | PT55S | PT5S | ✅ ISO-8601 |
| **RankingScheduler** | performFullSync | PT1H | PT5M | ✅ ISO-8601 |
| **RankingScheduler** | performIncrementalSync | PT10M | PT1M | ✅ ISO-8601 |
| **RankingScheduler** | performWeeklyRebuild | PT2H | PT10M | ✅ ISO-8601 |
| **MeetingStatusScheduler** | markFinishedMeetings | PT5M | PT30S | ✅ ISO-8601 |
| **RefreshTokenCleanupTask** | removeExpiredRefreshTokens | PT30M | PT2M | ✅ ISO-8601 |
| **PostImageFileScheduler** | deleteUnusedImageFiles | PT30M | PT5M | ✅ ISO-8601 |
| **FestivalScheduler** | syncNationalFestivalData | PT2H | PT1M | ✅ ISO-8601 |
| **FestivalScheduler** | syncSeoulFestivalData | PT1H | PT5M | ✅ ISO-8601 |

**결과**: 전체 7개 스케줄러, 9개 메서드 **모두 ISO-8601 포맷 사용** ✅

---

## 검증 방법

### 1. 빌드 테스트

```bash
./gradlew clean build
```

**예상 결과**: ✅ 빌드 성공 (기능 변경 없음, 표기법만 변경)

### 2. 테스트 실행

```bash
./gradlew test
```

**예상 결과**: ✅ 모든 테스트 통과 (동작 변경 없음)

### 3. 애플리케이션 시작

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

**예상 로그**:
```
[Scheduler] RankingScheduler - Application ready, starting initial ranking sync
[Scheduler] RankingScheduler - Initial ranking sync completed successfully
```

**확인 사항**:
- 스케줄러가 정상적으로 등록되고 실행됨
- ShedLock이 ISO-8601 포맷을 정상적으로 파싱함
- 기존 기능과 동일하게 동작

### 4. ShedLock 테이블 확인

```sql
SELECT name, locked_at, locked_until, locked_by
FROM shedlock
WHERE name LIKE '%Ranking%' OR name = 'retryFailedActivityEvents';
```

**예상 결과**:
- `locked_until` - `locked_at` = 설정된 `lockAtMostFor` 시간
- 예: performFullSync → 1시간 차이
- 예: retryFailedActivityEvents → 55초 차이

---

## 영향 분석

### 기능적 영향

**변경 없음** ✅
- ShedLock 라이브러리는 ISO-8601과 간편 표기법을 모두 지원
- 내부적으로 동일한 Duration 객체로 파싱됨
- 락 획득/해제 로직 동일

### 성능 영향

**변경 없음** ✅
- 파싱 비용 동일 (나노초 단위 차이)
- 런타임 동작 동일

### 호환성 영향

**향상됨** ✅
- ISO-8601은 Java 8+ Duration API와 100% 호환
- Spring Framework의 표준 Duration 파싱과 일치
- 다른 Spring Boot 프로젝트와 일관성 확보

---

## 참고 자료

### ISO-8601 Duration Format

**공식 표준**: [ISO 8601 - Wikipedia](https://en.wikipedia.org/wiki/ISO_8601#Durations)

**포맷 구조**:
```
P[n]Y[n]M[n]DT[n]H[n]M[n]S

P   = Period (필수 접두사)
T   = Time separator (날짜와 시간 구분)
Y/M/D = Years/Months/Days (날짜 부분)
H/M/S = Hours/Minutes/Seconds (시간 부분)
```

**예시**:
```
P3Y6M4DT12H30M5S  = 3년 6개월 4일 12시간 30분 5초
PT1H              = 1시간
PT15M             = 15분
P1DT12H           = 1일 12시간
```

### ShedLock 공식 문서

**GitHub**: [lukas-krecan/ShedLock](https://github.com/lukas-krecan/ShedLock)

**lockAtMostFor / lockAtLeastFor 설명**:
> You can use ISO 8601 Duration format or simple `"5m"`, `"1h"` format. We recommend using the ISO format.

**공식 권장사항**: ISO-8601 Duration 포맷 사용

### Java Duration API

**Java Docs**: [java.time.Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html)

```java
// Java Duration API는 ISO-8601을 표준으로 사용
Duration duration1 = Duration.parse("PT1H");    // 1시간
Duration duration2 = Duration.parse("PT30M");   // 30분
Duration duration3 = Duration.parse("PT55S");   // 55초
```

---

## 결론

### 수정 요약

**대상**: 2개 파일, 4개 스케줄러 메서드
**변경**: 시간 포맷 표기법 변경 (간편 표기 → ISO-8601)
**영향**: 없음 (동작 동일, 표준 준수)

### 효과

1. ✅ **국제 표준 준수**: ISO-8601 Duration Format
2. ✅ **코드 일관성**: 전체 9개 스케줄러 메서드 모두 동일 포맷
3. ✅ **가독성 향상**: 명확한 시간 표현 (PT prefix로 의미 명확화)
4. ✅ **ShedLock 권장사항 준수**: 공식 문서 권장 포맷 사용
5. ✅ **Spring Boot 생태계 일치**: Duration API와 동일한 표기법

### 다음 단계

수정사항은 `refactor/scheduler-code-review-fix` 브랜치에 반영되었습니다.

**권장 후속 조치**:
1. 빌드 및 테스트 검증 (`./gradlew clean build`)
2. 로컬 환경에서 스케줄러 동작 확인
3. ShedLock 테이블에서 락 시간 검증
4. 개발 서버 배포 후 모니터링

**향후 개선 방향**:
- ArchUnit 테스트 추가: `@SchedulerLock` 사용 시 ISO-8601 포맷 강제
- Checkstyle 규칙 추가: non-ISO Duration 포맷 감지

---

**작성일**: 2026-01-02
**작성자**: Claude Code
**관련 이슈**: SCHEDULER_CODE_REVIEW.md - SUGGESTION 1 (ShedLock 시간 포맷 통일)
