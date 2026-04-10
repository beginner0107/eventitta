#!/usr/bin/env python3
import argparse
import datetime as dt
from typing import Iterable, List, Sequence, Tuple


ADMIN_USER_ID = 900000
USER_ID_BASE = 1000000
POST_ID_BASE = 2000000
LIKE_ID_BASE = 3000000

ADMIN_PASSWORD_HASH = "$2b$10$vW54qubAwctuqnw9xun2fe/60nnmoL0XMgfkb86ZVV9umdyO3/F36"
SEEDED_USER_PASSWORD_HASH = ADMIN_PASSWORD_HASH
SEED_ACTOR = "perf-seed"
DEFAULT_REGIONS = [
    "1100000000",
    "1168000000",
    "2611000000",
    "2711000000",
    "2811000000",
]
TITLE_KEYWORDS = ["맛집", "러닝", "공연", "동네", "산책", "모임"]
CONTENT_KEYWORDS = ["커뮤니티", "이벤트", "후기", "핫플", "추천", "탐방"]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Emit deterministic SQL for Eventitta perf seed data."
    )
    parser.add_argument("--users", type=int, default=10000)
    parser.add_argument("--posts", type=int, default=100000)
    parser.add_argument("--likes", type=int, default=500000)
    parser.add_argument("--regions", type=str, default=",".join(DEFAULT_REGIONS))
    parser.add_argument("--include-admin", action="store_true", default=True)
    parser.add_argument("--skip-admin", action="store_true")
    parser.add_argument("--admin-email", type=str, default="perfadmin@example.com")
    parser.add_argument("--admin-password", type=str, default="Pass123!")
    return parser.parse_args()


def sql_value(value) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, dt.datetime):
        return f"'{value.strftime('%Y-%m-%d %H:%M:%S.%f')}'"
    text = str(value).replace("\\", "\\\\").replace("'", "''")
    return f"'{text}'"


def emit_insert(table: str, columns: Sequence[str], rows: Iterable[Tuple], batch_size: int = 1000):
    batch: List[Tuple] = []
    for row in rows:
        batch.append(row)
        if len(batch) >= batch_size:
            write_batch(table, columns, batch)
            batch = []
    if batch:
        write_batch(table, columns, batch)


def write_batch(table: str, columns: Sequence[str], rows: Sequence[Tuple]):
    values = ",\n  ".join(
        "(" + ", ".join(sql_value(value) for value in row) + ")" for row in rows
    )
    print(f"INSERT INTO {table} ({', '.join(columns)}) VALUES\n  {values};\n")


def user_created_at(index: int) -> dt.datetime:
    base = dt.datetime(2026, 1, 1, 9, 0, 0)
    return base + dt.timedelta(minutes=index % (60 * 24 * 30))


def post_created_at(index: int) -> dt.datetime:
    base = dt.datetime(2026, 2, 1, 8, 0, 0)
    return base + dt.timedelta(minutes=(index * 3) % (60 * 24 * 45))


def build_user_rows(user_count: int):
    for i in range(user_count):
        user_id = USER_ID_BASE + i
        created_at = user_created_at(i)
        points = ((i % 20) + 1) * 10
        latitude = round(37.540000 + (i % 200) * 0.0001, 6)
        longitude = round(127.040000 + (i % 200) * 0.0001, 6)
        yield (
            user_id,
            f"perfseed{i}@example.com",
            SEEDED_USER_PASSWORD_HASH,
            True,
            f"perfuser{i}",
            None,
            None,
            f"Perf seeded user {i}",
            None,
            f"서울특별시 성동구 성수동 {i}",
            latitude,
            longitude,
            points,
            "USER",
            "LOCAL",
            None,
            False,
            False,
            0,
            created_at,
            SEED_ACTOR,
            created_at,
            SEED_ACTOR,
        )


def build_admin_row(email: str):
    created_at = dt.datetime(2026, 1, 1, 0, 0, 0)
    return (
        ADMIN_USER_ID,
        email,
        ADMIN_PASSWORD_HASH,
        True,
        "perfadmin",
        None,
        None,
        "Perf admin account",
        None,
        "서울특별시 중구 세종대로 110",
        37.566500,
        126.978000,
        5000,
        "ADMIN",
        "LOCAL",
        None,
        False,
        False,
        0,
        created_at,
        SEED_ACTOR,
        created_at,
        SEED_ACTOR,
    )


def like_count_for_post(index: int, post_count: int, like_count: int) -> int:
    base = like_count // post_count
    extra = like_count % post_count
    return base + (1 if index < extra else 0)


def build_post_rows(post_count: int, like_count: int, user_count: int, region_codes: Sequence[str]):
    for i in range(post_count):
        post_id = POST_ID_BASE + i
        created_at = post_created_at(i)
        author_id = USER_ID_BASE + (i % user_count)
        keyword = TITLE_KEYWORDS[i % len(TITLE_KEYWORDS)]
        content_keyword = CONTENT_KEYWORDS[i % len(CONTENT_KEYWORDS)]
        likes = like_count_for_post(i, post_count, like_count)
        yield (
            post_id,
            author_id,
            f"[Perf] {keyword} 게시글 {i}",
            f"{keyword} 테스트 글 {i}입니다. {content_keyword} 검색과 지역 필터, 목록 조회 부하를 재현하기 위한 seeded content 입니다.",
            region_codes[i % len(region_codes)],
            likes,
            False,
            created_at,
            SEED_ACTOR,
            created_at,
            SEED_ACTOR,
        )


def build_post_like_rows(post_count: int, like_count: int, user_count: int):
    like_id = LIKE_ID_BASE
    for post_index in range(post_count):
        likes_for_post = like_count_for_post(post_index, post_count, like_count)
        post_id = POST_ID_BASE + post_index
        created_at = post_created_at(post_index)
        for offset in range(likes_for_post):
            user_id = USER_ID_BASE + ((post_index * 37 + offset) % user_count)
            yield (
                like_id,
                post_id,
                user_id,
                created_at,
                created_at,
            )
            like_id += 1


def build_gamification_stats_rows(user_count: int):
    for i in range(user_count):
        user_id = USER_ID_BASE + i
        updated_at = user_created_at(i)
        total_points = ((i % 20) + 1) * 10
        total_activity_count = 5 + (i % 40)
        yield (
            user_id,
            total_points,
            total_activity_count,
            updated_at,
        )


def build_activity_stats_rows(user_count: int):
    for i in range(user_count):
        user_id = USER_ID_BASE + i
        updated_at = user_created_at(i)
        yield (
            user_id,
            "CREATE_POST",
            1 + (i % 10),
            10 + ((i % 10) * 10),
            updated_at,
        )
        yield (
            user_id,
            "CREATE_COMMENT",
            2 + (i % 15),
            10 + ((i % 15) * 5),
            updated_at,
        )


def main():
    args = parse_args()
    include_admin = args.include_admin and not args.skip_admin
    region_codes = [code.strip() for code in args.regions.split(",") if code.strip()]
    if not region_codes:
        raise SystemExit("at least one region code is required")
    if args.users <= 0 or args.posts <= 0:
        raise SystemExit("--users and --posts must be positive")
    if args.likes < 0:
        raise SystemExit("--likes must be zero or positive")
    if args.likes > args.users * args.posts:
        raise SystemExit("--likes is too large for the requested users/posts combination")

    print("-- Eventitta perf seed SQL")
    print(f"-- users={args.users}, posts={args.posts}, likes={args.likes}")
    print(f"-- user id range={USER_ID_BASE}..{USER_ID_BASE + args.users - 1}")
    print(f"-- post id range={POST_ID_BASE}..{POST_ID_BASE + args.posts - 1}")
    print(f"-- like id range={LIKE_ID_BASE}..{LIKE_ID_BASE + max(args.likes - 1, 0)}")
    if include_admin:
        print(f"-- admin account: {args.admin_email} / {args.admin_password}")
    print("SET NAMES utf8mb4;")
    print("SET FOREIGN_KEY_CHECKS = 0;\n")

    user_columns = [
        "id",
        "email",
        "password",
        "email_verified",
        "nickname",
        "profile_picture_media_id",
        "profile_picture_url",
        "self_intro",
        "interests",
        "address",
        "latitude",
        "longitude",
        "points",
        "role",
        "provider",
        "provider_id",
        "deleted",
        "suspended",
        "auth_version",
        "created_at",
        "created_by",
        "updated_at",
        "updated_by",
    ]

    if include_admin:
        emit_insert("users", user_columns, [build_admin_row(args.admin_email)], batch_size=1)

    emit_insert("users", user_columns, build_user_rows(args.users), batch_size=500)

    emit_insert(
        "posts",
        [
            "id",
            "user_id",
            "title",
            "content",
            "region_code",
            "like_count",
            "deleted",
            "created_at",
            "created_by",
            "updated_at",
            "updated_by",
        ],
        build_post_rows(args.posts, args.likes, args.users, region_codes),
        batch_size=1000,
    )

    emit_insert(
        "post_likes",
        ["id", "post_id", "user_id", "created_at", "updated_at"],
        build_post_like_rows(args.posts, args.likes, args.users),
        batch_size=2000,
    )

    emit_insert(
        "user_gamification_stats",
        ["user_id", "total_points", "total_activity_count", "updated_at"],
        build_gamification_stats_rows(args.users),
        batch_size=1000,
    )

    emit_insert(
        "user_activity_stats",
        ["user_id", "action_type", "action_count", "points_total", "updated_at"],
        build_activity_stats_rows(args.users),
        batch_size=2000,
    )

    print("SET FOREIGN_KEY_CHECKS = 1;")


if __name__ == "__main__":
    main()
