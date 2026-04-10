#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/infra/docker-compose.perf.yml"
USERS=10000
POSTS=100000
LIKES=500000
DB_USER="${PERF_DB_USER:-eventittaUser}"
DB_PASSWORD="${PERF_DB_PASSWORD:-eventittaPass}"
DB_NAME="${PERF_DB_NAME:-eventitta_perf}"
RESTART_APPS=true
APP1_HEALTH_URL="${PERF_APP1_HEALTH_URL:-http://localhost:${PERF_APP1_PORT:-18081}/actuator/health}"
APP2_HEALTH_URL="${PERF_APP2_HEALTH_URL:-http://localhost:${PERF_APP2_PORT:-18082}/actuator/health}"
NGINX_HEALTH_URL="${PERF_NGINX_HEALTH_URL:-http://localhost:${PERF_NGINX_PORT:-18080}/actuator/health}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --compose-file)
      COMPOSE_FILE="$2"; shift 2 ;;
    --users)
      USERS="$2"; shift 2 ;;
    --posts)
      POSTS="$2"; shift 2 ;;
    --likes)
      LIKES="$2"; shift 2 ;;
    --db-user)
      DB_USER="$2"; shift 2 ;;
    --db-password)
      DB_PASSWORD="$2"; shift 2 ;;
    --db-name)
      DB_NAME="$2"; shift 2 ;;
    --no-restart-apps)
      RESTART_APPS=false; shift ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1 ;;
  esac
done

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "Docker Compose not found" >&2
  exit 1
fi

MYSQL_CMD=("${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" exec -T db mysql "-u${DB_USER}" "-p${DB_PASSWORD}" "$DB_NAME")

wait_for_http() {
  local url="$1"
  local name="$2"
  local max_attempts="${3:-60}"

  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "[perf-seed] ${name} is healthy (${url})"
      return 0
    fi
    sleep 1
  done

  echo "[perf-seed] ${name} did not become healthy in time (${url})" >&2
  return 1
}

echo "[perf-seed] waiting for db in ${COMPOSE_FILE}"
"${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" exec -T db mysqladmin ping -h 127.0.0.1 "-u${DB_USER}" "-p${DB_PASSWORD}" --silent >/dev/null

echo "[perf-seed] clearing previous perf seed ranges"
cat <<'SQL' | "${MYSQL_CMD[@]}"
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM user_activity_stats WHERE user_id = 900000 OR user_id BETWEEN 1000000 AND 1999999;
DELETE FROM user_gamification_stats WHERE user_id = 900000 OR user_id BETWEEN 1000000 AND 1999999;
DELETE FROM post_likes WHERE id BETWEEN 3000000 AND 3999999999;
DELETE FROM posts WHERE id BETWEEN 2000000 AND 2999999999;
DELETE FROM users WHERE id = 900000 OR id BETWEEN 1000000 AND 1999999;
SET FOREIGN_KEY_CHECKS = 1;
SQL

echo "[perf-seed] loading seed data users=${USERS} posts=${POSTS} likes=${LIKES}"
python3 "${PROJECT_ROOT}/scripts/perf/seed_perf_data.py" \
  --users "${USERS}" \
  --posts "${POSTS}" \
  --likes "${LIKES}" \
  | "${MYSQL_CMD[@]}"

if [[ "${RESTART_APPS}" == "true" ]]; then
  echo "[perf-seed] restarting app replicas so startup ranking rebuild sees fresh stats"
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" restart app-1 app-2 >/dev/null
  wait_for_http "${APP1_HEALTH_URL}" "app-1"
  wait_for_http "${APP2_HEALTH_URL}" "app-2"
  wait_for_http "${NGINX_HEALTH_URL}" "nginx"
fi

echo "[perf-seed] completed"
