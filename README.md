# Eventitta

> 지역 기반 소셜 커뮤니티 플랫폼

**개발 기간**: 2025.05 ~ 진행 중

Eventitta는 지역 주민들이 온라인에서 관심사와 정보를 공유하고, 오프라인 이벤트나 모임으로 자연스럽게 연결될 수 있는 환경을 제공하는 Spring Boot 기반 소셜 커뮤니티 플랫폼입니다.

## 기술 스택

### Backend

- **Java 17** + **Spring Boot 3.4.5**
- **Spring Data JPA** + **QueryDSL 5.0.0**
- **Spring Security** + **JWT**
- **MySQL 8.0** / **H2**

### DevOps & Tools

- **Gradle** (빌드)
- **Docker** (컨테이너화)
- **ShedLock 6.6.0** (분산 스케줄링)
- **Swagger/OpenAPI** (API 문서화)

## 주요 기능

- **JWT 인증/인가**: Access/Refresh 토큰 기반
- **커뮤니티**: 게시글 작성, 댓글, 좋아요, 이미지 업로드
- **모임 관리**: 모임 생성, 참가 신청, 승인 시스템
- **축제 정보**: 전국/서울시 축제 데이터 자동 동기화
- **게임화**: 사용자 활동 포인트, 배지 시스템
- **위치 기반**: 지역별 콘텐츠 분류 및 거리 기반 검색
- **대시보드**: 사용자 활동 통계 및 랭킹

## 프로젝트 구조

```
src/main/java/com/eventitta/
├── EventittaApplication.java
├── auth/                 # JWT 인증/인가, 토큰 관리
├── comment/              # 계층형 댓글 시스템
├── common/               # 공통 설정, 예외 처리, 유틸리티
├── dashboard/            # 사용자 활동 통계 및 랭킹
├── festivals/            # 축제 정보 관리, 외부 API 연동
├── file/                 # 파일 업로드 및 저장
├── gamification/         # 포인트, 배지, 사용자 활동 추적
├── meeting/              # 모임 생성, 참가 관리
├── post/                 # 게시글, 좋아요, 이미지
├── region/               # 행정구역 기반 지역 관리
└── user/                 # 사용자 프로필 관리
```

## 핵심 엔티티

| 엔티티              | 설명                       |
|------------------|--------------------------|
| **User**         | 사용자 정보, JWT 인증, 프로필 관리   |
| **Post**         | 게시글, 좋아요, 이미지 첨부, 지역 분류  |
| **Comment**      | 계층형 댓글 시스템, 대댓글 지원       |
| **Meeting**      | 모임 정보, 참가자 관리, 상태 추적     |
| **Festival**     | 축제 정보, 외부 API 데이터, 위치 기반 |
| **UserActivity** | 사용자 활동 추적, 포인트 적립        |
| **Badge**        | 배지 시스템, 활동 기반 자동 지급      |
| **Region**       | 행정구역 코드, 계층형 지역 관리       |

## API 엔드포인트

### 인증 `/api/v1/auth`

- `POST /signup` - 회원가입
- `POST /login` - 로그인 (JWT 토큰 발급)
- `POST /refresh` - 토큰 갱신
- `POST /logout` - 로그아웃

### 커뮤니티 `/api/v1/posts`

- `GET /` - 게시글 목록 (페이징, 필터링)
- `POST /` - 게시글 작성
- `GET /{id}` - 게시글 상세
- `PUT /{id}` - 게시글 수정
- `DELETE /{id}` - 게시글 삭제
- `POST /{id}/like` - 좋아요 토글

### 모임 `/api/v1/meetings`

- `GET /` - 모임 검색
- `POST /` - 모임 생성
- `POST /{id}/join` - 모임 참가 신청
- `PUT /{id}/participants/{pid}/approve` - 참가 승인

### 축제 `/api/v1/festivals`

- `GET /nearby` - 위치 기반 축제 검색

### 파일 `/api/v1/uploads`

- `POST /images` - 이미지 업로드

## 외부 API 연동

### 서울시 문화행사 API

- **URL**: `http://openapi.seoul.go.kr:8088`
- **스케줄**: 매일 03:00 (KST) 자동 동기화
- **데이터**: 서울시 문화행사, 공연, 전시 정보

### 전국 축제 API

- **URL**: `http://api.data.go.kr/openapi/tn_pubr_public_cltur_fstvl_api`
- **스케줄**: 분기별 (1/4/7/10월 1일 02:00) 자동 동기화
- **데이터**: 전국 문화축제 정보

## 스케줄러 작업 (ShedLock 적용)

| 작업         | 주기     | 설명                |
|------------|--------|-------------------|
| 축제 데이터 동기화 | 일별/분기별 | 외부 API에서 축제 정보 수집 |
| 토큰 정리      | 매시간    | 만료된 Refresh 토큰 삭제 |
| 모임 상태 업데이트 | 매분     | 종료된 모임 상태 자동 변경   |

## 게임화 시스템

### 포인트 시스템

- 게시글 작성: **10 포인트**
- 댓글 작성: **5 포인트**
- 좋아요 받기: **1 포인트**
- 모임 참가: **20 포인트**

### 배지 시스템

- **첫 게시글**: 첫 번째 게시글 작성
- **열혈 댓글러**: 10개 이상 댓글 작성
- **첫 모임 참가**: 첫 번째 모임 참가
- **프로 좋아요꾼**: 50개 이상 좋아요 받기

## 로컬 실행

### 필요 조건

- Java 17+
- MySQL 8.0+
- Gradle 7.0+

### 실행 방법

1. **저장소 클론**

```bash
git clone https://github.com/your-username/eventitta.git
cd eventitta
```

2. **데이터베이스 설정**

```bash
cd infra
docker-compose up -d
```

3. **환경 변수 설정**

```bash
export MYSQL_PASSWORD=your-password
export SECRET_KEY=your-jwt-secret
export SEOUL_API_KEY=your-seoul-api-key
export NATIONAL_API_KEY=your-national-api-key
```

4. **애플리케이션 실행**

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 빌드 및 테스트

```bash
# 프로젝트 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 코드 스타일 검사
./gradlew editorconfigCheck
```

## 모니터링

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **SQL 로깅**: P6Spy를 통한 쿼리 로깅
- **응답 시간**: ResponseTimeFilter로 API 성능 측정

## 보안

- **JWT 인증**: Access Token (1시간) + Refresh Token (24시간)
- **쿠키 보안**: HttpOnly, Secure 플래그 적용
- **비밀번호 암호화**: BCrypt 해싱

## 최근 업데이트

- ShedLock 적용: 분산 환경에서 스케줄러 중복 실행 방지
- User 엔티티 개선: Primary Key 자료형 최적화
- 축제 데이터 리팩토링: 중복 감지 및 데이터 품질 향상
- 통합 테스트 강화: 스케줄러 동시 실행 테스트 추가

## 개발 진행 상황

### 완료

- JWT 기반 인증/인가 시스템
- 사용자 관리 및 프로필
- 게시글 CRUD 및 좋아요 시스템
- 계층형 댓글 시스템
- 모임 생성 및 참가 관리
- 축제 정보 외부 API 연동
- 게임화 시스템 (포인트, 배지)
- 파일 업로드 기능
- 위치 기반 검색
- ShedLock 분산 스케줄링

### 진행 예정

- 실시간 알림 시스템
- 성능 최적화 및 캐싱

## 라이센스

개인 프로젝트
