# 이벤트있다 Eventitta

- 이벤트있다(Eventitta)는 지역 기반 소셜 커뮤니티 플랫폼으로, 온라인 상에서 지역 주민들이 서로의 관심사와 정보를 공유하고,
- 이를 바탕으로 오프라인 이벤트나 모임으로 자연스럽게 연결될 수 있는 환경을 제공하는 서비스입니다.

## 1. 프로젝트 개요

* **제작 기간**: 2025-04-27 \~ (진행 중)
* **참여 인원**: 1명 (개인 프로젝트)
* **프로젝트 설명**:

## 2. 사용 기술 스택

### Back-end

* Java 17, Spring Boot 3.4.5, Gradle
* Spring Data JPA, MySQL

## 3. 아키텍처 설계

## 4. 도메인 모델

## 5. 차트 & 다이어그램

<details>
  <summary> ERD 보기/숨기기</summary>

```mermaid
erDiagram
    USERS {
        INT id PK
        VARCHAR email
        VARCHAR password
        VARCHAR nickname
        VARCHAR profile_picture_url
        TEXT self_intro
        JSON interests
        VARCHAR address
        DECIMAL latitude
        DECIMAL longitude
        ENUM role
        VARCHAR provider
        VARCHAR provider_id
        DATETIME created_at
        DATETIME updated_at
    }

    REGION {
        VARCHAR code PK
        VARCHAR name
        VARCHAR parent_code
        INT level
    }

    POSTS {
        INT id PK
        INT user_id FK
        VARCHAR title
        TEXT content
        VARCHAR region_code FK
        DATETIME created_at
        DATETIME updated_at
    }

    COMMENTS {
        INT id PK
        INT post_id FK
        INT user_id FK
        TEXT comment
        INT parent_comment_id FK
        DATETIME created_at
        DATETIME updated_at
    }

    BADGES {
        INT id PK
        VARCHAR badge_name
        TEXT description
        VARCHAR criteria
        DATETIME created_at
        DATETIME updated_at
    }

    USER_BADGES {
        INT id PK
        INT user_id FK
        INT badge_id FK
        DATETIME awarded_at
    }

    ACTIVITY_TYPE {
        INT id PK
        VARCHAR code
        VARCHAR name
        DATETIME created_at
        DATETIME updated_at
    }

    USER_ACTIVITY {
        INT id PK
        INT activity_type_id FK
        INT user_id FK
        INT points
        DATETIME activity_date
    }

    MEETINGS {
        INT id PK
        INT user_id FK
        VARCHAR title
        TEXT description
        DATETIME start_time
        DATETIME end_time
        INT max_members
        ENUM status
        VARCHAR location_address
        DECIMAL latitude
        DECIMAL longitude
        DATETIME created_at
        DATETIME updated_at
    }

    MEETING_PARTICIPANTS {
        INT id PK
        INT meeting_id FK
        INT user_id FK
        ENUM join_status
        DATETIME created_at
        DATETIME updated_at
    }

    EVENTS {
        INT id PK
        VARCHAR source
        VARCHAR title
        VARCHAR place
        VARCHAR address
        TEXT description
        DATETIME start_time
        DATETIME end_time
        VARCHAR category
        BOOLEAN is_free
        VARCHAR use_fee
        VARCHAR homepage_url
        VARCHAR main_img_url
        DATETIME created_at
        DATETIME updated_at
    }

    REFRESH_TOKENS {
        INT id PK
        INT user_id FK
        VARCHAR token_hash
        DATETIME created_at
        DATETIME expires_at
    }

    USERS ||--o{ POSTS: "writes"
    USERS ||--o{ COMMENTS: "writes"
    USERS ||--o{ USER_BADGES: "receives"
    USERS ||--o{ USER_ACTIVITY: "logs"
    USERS ||--o{ MEETINGS: "hosts"
    USERS ||--o{ MEETING_PARTICIPANTS: "joins"
    USERS ||--o{ REFRESH_TOKENS: "owns"
    REGION ||--o{ POSTS: "has posts"
    POSTS ||--o{ COMMENTS: "contains"
    COMMENTS ||--o{ COMMENTS: "replies_to"
    BADGES ||--o{ USER_BADGES: "grants"
    ACTIVITY_TYPE ||--o{ USER_ACTIVITY: "categorizes"
    MEETINGS ||--o{ MEETING_PARTICIPANTS: "includes"
```

</details>

## 6. 핵심 기능

## 7. Technical Issues & 고민 사항

- 인증/인가 구현: 세션, 토큰 방식
- Geo 검색 (위경도 기반 범위·거리 조회)

## 8. 트러블 슈팅

