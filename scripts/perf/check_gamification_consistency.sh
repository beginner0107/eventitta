#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/infra/docker-compose.perf.yml"
BASE_URL="${BASE_URL:-http://localhost:18080}"
DB_USER="${PERF_DB_USER:-eventittaUser}"
DB_PASSWORD="${PERF_DB_PASSWORD:-eventittaPass}"
DB_NAME="${PERF_DB_NAME:-eventitta_perf}"
ADMIN_EMAIL="${PERF_ADMIN_EMAIL:-perfadmin@example.com}"
ADMIN_PASSWORD="${PERF_ADMIN_PASSWORD:-Pass123!}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --compose-file)
      COMPOSE_FILE="$2"; shift 2 ;;
    --base-url)
      BASE_URL="$2"; shift 2 ;;
    --db-user)
      DB_USER="$2"; shift 2 ;;
    --db-password)
      DB_PASSWORD="$2"; shift 2 ;;
    --db-name)
      DB_NAME="$2"; shift 2 ;;
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

DB_COUNTS="$("${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" exec -T db mysql "-u${DB_USER}" "-p${DB_PASSWORD}" "$DB_NAME" -Nse \
  "SELECT COUNT(*) FROM user_gamification_stats WHERE total_points > 0; SELECT COUNT(*) FROM user_gamification_stats WHERE total_activity_count > 0;")"

DB_POINTS_COUNT="$(printf '%s\n' "${DB_COUNTS}" | sed -n '1p')"
DB_ACTIVITY_COUNT="$(printf '%s\n' "${DB_COUNTS}" | sed -n '2p')"

COOKIE_JAR="$(mktemp)"
trap 'rm -f "${COOKIE_JAR}"' EXIT

curl -fsS \
  -c "${COOKIE_JAR}" \
  -X POST "${BASE_URL%/}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  --data "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}\"}" >/dev/null

API_RESPONSE="$(curl -fsS -b "${COOKIE_JAR}" "${BASE_URL%/}/api/v1/rankings/stats")"
read -r API_POINTS_COUNT API_ACTIVITY_COUNT < <(
  python3 - <<'PY' "${API_RESPONSE}"
import json
import sys
payload = json.loads(sys.argv[1])
print(payload.get("pointsRankingCount", 0), payload.get("activityRankingCount", 0))
PY
)

echo "[consistency] db.points=${DB_POINTS_COUNT} api.points=${API_POINTS_COUNT}"
echo "[consistency] db.activity=${DB_ACTIVITY_COUNT} api.activity=${API_ACTIVITY_COUNT}"

if [[ "${DB_POINTS_COUNT}" != "${API_POINTS_COUNT}" || "${DB_ACTIVITY_COUNT}" != "${API_ACTIVITY_COUNT}" ]]; then
  echo "[consistency] mismatch detected" >&2
  exit 1
fi

echo "[consistency] ranking projection matches aggregate stats"
