# Region 데이터 관리

행정구역(시/도/구/군/읍/면/동) 데이터 생성 및 관리를 위한 스크립트

## 🎯 관심사 분리 (Separation of Concerns)

### ✅ Flyway: 스키마 관리 (Schema Management)

- **책임**: `regions` 테이블 구조 정의 및 변경
- **파일**: `V1__Create_schema.sql`, `V*__Alter_regions.sql`
- **원칙**: **단일 진실의 원천 (Single Source of Truth)**

```sql
-- Flyway가 관리
CREATE TABLE regions (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_code VARCHAR(10),
    level INT NOT NULL,
    -- 미래에 컬럼 추가/삭제
);
```

### ✅ Python: 데이터 관리 (Data Management)

- **책임**: CSV → INSERT SQL 생성
- **파일**: `generate_region_sql.py`
- **원칙**: 스키마는 건드리지 않음, **`CREATE TABLE LIKE regions`** 활용

```python
# Python이 생성 (스키마는 Flyway 것 사용)
CREATE TABLE regions_new LIKE regions;  -- ← 자동 복사!
INSERT INTO regions_new VALUES ...;
```

### 💡 왜 이렇게 설계했나?

**문제**: regions 테이블에 `deleted_at` 컬럼 추가하면?

```sql
-- V11__Add_region_deleted_at.sql
ALTER TABLE regions ADD COLUMN deleted_at TIMESTAMP NULL;
```

**해결**:
- ❌ **이전 방식**: Python 스크립트에 `deleted_at` 추가해야 함 (이중 관리)
- ✅ **현재 방식**: `CREATE TABLE LIKE regions`가 자동으로 반영! (Python 수정 불필요)

**결과**:
- ✅ 신규 개발자는 **Flyway만** 보면 됨
- ✅ 스키마 변경 시 **Python 수정 불필요**
- ✅ 동기화 문제 **원천 차단**

---

## 📁 파일 설명

- `regions_source_20250805.csv` - 국토교통부 법정동 코드 원본 데이터 (2025년 8월 5일 기준)
- `generate_region_sql.py` - SQL INSERT 문 자동 생성 스크립트 (스키마는 생성 안 함)
- **생성 결과**: `src/main/resources/db/migration/V9__Fix_region_hierarchy.sql`

## 🌐 데이터 출처

- **제공**: 국토교통부
- **명칭**: 법정동코드 전체자료
- **다운로드**: [국가공간정보포털](https://www.nsdi.go.kr/lxmap/index.do)
- **버전**: 2025년 8월 5일 기준
- **라이선스**: 공공데이터 (자유 이용)

## 🚀 배포 전략

### ⭐ RENAME 전략 (무중단 배포) - 현재 사용 중

**작동 방식**:
1. `regions_new` 임시 테이블 생성
2. 새 데이터를 `regions_new`에 INSERT (1,860개)
3. **원자적 교체** (1ms 이하):
   ```sql
   RENAME TABLE
       regions TO regions_old,
       regions_new TO regions;
   ```
4. `regions_old` 백업 보관 (롤백 가능)

**장점**:
- ✅ **다운타임 0초** - API 중단 없음
- ✅ **외래 키 안전** - posts, meetings 영향 없음
- ✅ **롤백 가능** - regions_old로 즉시 복구
- ✅ **트랜잭션 안전** - 원자적 연산

**롤백 방법**:
```sql
-- 문제 발생 시 즉시 롤백
RENAME TABLE
    regions TO regions_failed,
    regions_old TO regions;
```

**검증 후 정리**:
```sql
-- 검증 완료 후 옛날 테이블 삭제
DROP TABLE IF EXISTS regions_old;
```

---

## 🏗️ 데이터 구조

### 행정구역 계층 (3단계)

```
Level 1: 시/도          (17개)    예: 1100000000 (서울특별시)
  └─ Level 2: 시/군/구  (~250개)  예: 1111000000 (종로구)
      └─ Level 3: 읍/면/동 (~3,500개) 예: 1111010100 (청운동)
```

### 코드 체계

- **길이**: 10자리 문자열
- **형식**: 앞자리부터 의미있는 값, 뒷자리는 0으로 채움

  ```
  1111010100
  ├─ 11: 서울특별시
  ├─ 1111: 종로구
  └─ 1111010100: 청운동
  ```

### ParentCode 규칙

- Level 1 (시/도): `NULL`
- Level 2 (시/군/구): 앞 2자리 + `00000000` (예: `1100000000`)
- Level 3 (읍/면/동): 앞 5자리 + `00000` (예: `1111000000`)

## 🚀 사용 방법

### 초기 데이터 생성 (이미 완료됨)

```bash
# 프로젝트 루트에서 실행
python3 scripts/region-data/generate_region_sql.py
```

**결과**:
- `src/main/resources/db/migration/V9__Fix_region_hierarchy.sql` 생성
- 약 3,800개 레코드 (시/도 17 + 시/군/구 250 + 읍/면/동 3,500)

### 스키마 변경 (regions 테이블 구조 수정)

**예시: deleted_at 컬럼 추가**

```sql
-- V11__Add_region_deleted_at.sql
ALTER TABLE regions ADD COLUMN deleted_at TIMESTAMP NULL;
```

✅ **Python 스크립트 수정 불필요!**
- `CREATE TABLE LIKE regions`가 자동으로 새 스키마 복사
- 다음 데이터 업데이트 시 자동 반영

❌ **수정하면 안 되는 곳**:
- `generate_region_sql.py` - 스키마 코드 없음!
- INSERT 문만 생성하므로 영향 없음

### 데이터 업데이트 (연 1-2회 행정구역 개편 시)

1. **최신 법정동 코드 다운로드**
   - [국가공간정보포털](https://www.nsdi.go.kr/lxmap/index.do) 접속
   - "법정동코드 전체자료" 다운로드 (CSV)

2. **파일 교체**
   ```bash
   # 새 CSV를 영문명으로 저장
   mv "국토교통부_법정동코드_YYYYMMDD.csv" scripts/region-data/regions_source_YYYYMMDD.csv
   ```

3. **스크립트 수정**
   ```python
   # generate_region_sql.py의 csv_file 경로 변경
   csv_file = os.path.join(script_dir, "regions_source_YYYYMMDD.csv")
   ```

4. **SQL 생성**
   ```bash
   python3 scripts/region-data/generate_region_sql.py
   ```

5. **새 마이그레이션 생성**
   ```bash
   # V10, V11... 등으로 새 마이그레이션 파일 생성
   # 예: V10__Update_regions_20260101.sql
   ```

6. **애플리케이션 재시작**
   ```bash
   ./gradlew clean bootRun
   ```

## ⚙️ 스크립트 설정

### 레벨 제한

현재는 **Level 3까지만** 포함 (리(里) 제외):

```python
max_level = 3  # 리(4) 제외
```

리(里)까지 포함하려면:
```python
max_level = 4  # 전체 포함 (~42,000개)
```

### 배치 크기

INSERT 문 배치 크기 (기본: 500개):

```python
batch_size = 500  # 한 번에 INSERT할 레코드 수
```

## 📊 생성된 데이터 통계

```
Level 1 (시/도):     17개
Level 2 (시/군/구):  약 250개
Level 3 (읍/면/동):  약 3,500개
------------------------
총계:                약 3,800개
```

## 🔍 검증 방법

### 1. 데이터베이스 확인

```sql
-- 레벨별 개수
SELECT level, COUNT(*) as count
FROM regions
GROUP BY level
ORDER BY level;

-- 계층 구조 확인 (청운동 예시)
SELECT
    r1.name as '시/도',
    r2.name as '시/군/구',
    r3.name as '읍/면/동'
FROM regions r3
LEFT JOIN regions r2 ON r3.parent_code = r2.code
LEFT JOIN regions r1 ON r2.parent_code = r1.code
WHERE r3.code = '1111010100';
```

### 2. API 테스트

```bash
# 청운동 계층 구조 조회
curl -s http://localhost:8080/api/v1/regions/1111010100/hierarchy | jq .

# 예상 결과:
# [
#   { "code": "1100000000", "name": "서울특별시", "level": 1 },
#   { "code": "1111000000", "name": "서울특별시 종로구", "level": 2 },
#   { "code": "1111010100", "name": "서울특별시 종로구 청운동", "level": 3 }
# ]
```

## ⚠️ 주의사항

### 🔥 배포 전략 선택

**현재 설정**: RENAME 전략 (무중단)
```python
use_rename_strategy = True  # 기본값 (권장)
```

**DELETE 전략으로 변경** (비추천):
```python
use_rename_strategy = False  # 다운타임 발생
```

### ✅ RENAME 전략 (권장)

- **안전함**: 외래 키 영향 없음
- **무중단**: API 다운타임 0초
- **롤백 가능**: regions_old 보관
- **프로덕션 적합**: 가장 안전한 방법

### ⚠️ DELETE 전략 (비권장)

- **위험**: 외래 키 제약조건 위반 가능
- **다운타임**: DELETE와 INSERT 사이 API 에러
- **롤백 불가**: 데이터 손실 위험
- **테스트 환경 전용**: 프로덕션 사용 금지

### 외래 키 제약조건

**RENAME 전략**:
- MySQL은 RENAME TABLE 시 외래 키 자동 업데이트
- 테이블명만 바뀌고 제약조건 유지
- 안전함 ✅

**DELETE 전략**:
```sql
SET FOREIGN_KEY_CHECKS = 0;  -- 위험!
DELETE FROM regions WHERE 1=1;
SET FOREIGN_KEY_CHECKS = 1;
```
- 외래 키 참조 테이블(posts, meetings)에 영향
- 데이터 손실 가능 ⚠️

### 한글 파일명 문제

- CSV 원본 파일명이 한글인 경우 Git 커밋 시 문제 발생 가능
- **반드시 영문으로 변경**: `regions_source_YYYYMMDD.csv`

### 인코딩

- 스크립트는 자동으로 인코딩 감지 (`utf-8`, `cp949`, `euc-kr` 등)
- 문제 발생 시 CSV를 `UTF-8`로 저장 후 재시도

### 구분자

- 탭(`\t`) 또는 콤마(`,`) 자동 감지
- 국토교통부 CSV는 보통 **탭 구분자** 사용

### Flyway 마이그레이션

- 버전 번호는 **순차적**으로 증가해야 함
- 이미 실행된 마이그레이션은 **절대 수정 금지**
- 데이터 업데이트는 **새 버전**으로 생성 (V10, V11...)

## 🐛 문제 해결

### "CSV 필드명을 읽을 수 없습니다"

→ CSV 인코딩 문제. UTF-8로 저장 후 재시도

### "필수 필드를 찾을 수 없습니다"

→ CSV 파일 형식 확인. 필드명에 "법정동코드", "법정동명" 포함되어야 함

### "SQL 파일이 비어있습니다"

→ max_level 설정 확인. 너무 낮으면 데이터가 필터링됨

### 한글 깨짐

→ CSV 인코딩을 `UTF-8 (BOM 없음)`으로 저장

## 📚 참고 자료

- [국가공간정보포털](https://www.nsdi.go.kr/lxmap/index.do)
- [행정안전부 행정표준코드관리시스템](https://www.code.go.kr/)
- [Flyway 마이그레이션 가이드](https://flywaydb.org/documentation/)

---

**최종 업데이트**: 2025-11-10
**작성자**: Eventitta Team
