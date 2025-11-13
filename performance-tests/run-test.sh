#!/bin/bash

###############################################################################
# Region API 성능 테스트 실행 스크립트
#
# 사용법:
#   ./performance-tests/run-test.sh [명령어] [옵션]
#
# 명령어:
#   start     - 환경 시작 (InfluxDB + Grafana)
#   test      - 성능 테스트 실행
#   stop      - 환경 종료
#   clean     - 데이터 삭제 (볼륨 포함)
#   all       - 전체 실행 (start + test)
#
# 옵션(공통):
#   --base-url <url>      대상 애플리케이션 BASE URL (기본: http://localhost:8080)
#   --influx-url <url>    InfluxDB 수집 URL (기본: http://localhost:8086/k6)
#   --grafana-url <url>   Grafana 접속 URL (기본: http://localhost:3000)
#   test 명령의 태그 인자(true/false)는 하위 호환 유지
#
# 환경변수:
#   BASE_URL 또는 TARGET_BASE_URL    --base-url와 동일(옵션보다 낮은 우선순위)
#   PERF_INFLUX_URL                  --influx-url와 동일(옵션보다 낮은 우선순위)
#   PERF_GRAFANA_URL                 --grafana-url와 동일(옵션보다 낮은 우선순위)
###############################################################################

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 디렉토리 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
INFRA_DIR="${PROJECT_ROOT}/infra"

# 공통 프린트 함수
print_header() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

print_success() { echo -e "${GREEN}✓${NC} $1"; }
print_error() { echo -e "${RED}✗${NC} $1"; }
print_warning() { echo -e "${YELLOW}⚠${NC} $1"; }
print_info() { echo -e "${BLUE}ℹ${NC} $1"; }

# 기본값
DEFAULT_APP_URL="http://localhost:8080"
DEFAULT_INFLUX_URL="http://localhost:8086/k6"
DEFAULT_GRAFANA_URL="http://localhost:3000"

# 인자 파싱(명령어 + 옵션)
CMD="${1:-}"
shift || true

APP_BASE_URL="${BASE_URL:-${TARGET_BASE_URL:-$DEFAULT_APP_URL}}"
INFLUX_URL="${PERF_INFLUX_URL:-$DEFAULT_INFLUX_URL}"
GRAFANA_URL="${PERF_GRAFANA_URL:-$DEFAULT_GRAFANA_URL}"
CACHE_TAG_DEFAULT="false"

ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      APP_BASE_URL="$2"; shift 2 ;;
    --influx-url)
      INFLUX_URL="$2"; shift 2 ;;
    --grafana-url)
      GRAFANA_URL="$2"; shift 2 ;;
    true|false)
      # test 명령 호환 태그
      CACHE_TAG_DEFAULT="$1"; shift ;;
    *)
      ARGS+=("$1"); shift ;;
  esac
done

# URL 정규화: 스킴/슬래시 보정, influx /k6 보장
normalize_urls() {
  # APP_BASE_URL
  if [[ ! "$APP_BASE_URL" =~ ^https?:// ]]; then APP_BASE_URL="http://$APP_BASE_URL"; fi
  APP_BASE_URL="${APP_BASE_URL%/}"
  # INFLUX_URL
  if [[ ! "$INFLUX_URL" =~ ^https?:// ]]; then INFLUX_URL="http://$INFLUX_URL"; fi
  if [[ "$INFLUX_URL" =~ ^https?://[^/]+$ ]]; then INFLUX_URL="$INFLUX_URL/k6"; fi
  INFLUX_URL="${INFLUX_URL%/}"
  # GRAFANA_URL
  if [[ ! "$GRAFANA_URL" =~ ^https?:// ]]; then GRAFANA_URL="http://$GRAFANA_URL"; fi
  GRAFANA_URL="${GRAFANA_URL%/}"
}

check_requirements() {
    normalize_urls
    print_header "환경 확인 중..."
    if ! command -v k6 &> /dev/null; then
        print_error "k6가 설치되지 않았습니다."
        echo ""
        echo "설치 방법:"
        echo "  macOS:   brew install k6"
        echo "  Windows: choco install k6"
        echo "  Linux:   https://k6.io/docs/get-started/installation/"
        exit 1
    fi
    print_success "k6 설치됨 ($(k6 version | head -n1))"
    if ! command -v docker &> /dev/null; then
        print_error "Docker가 설치되지 않았습니다."
        exit 1
    fi
    print_success "Docker 설치됨 ($(docker --version))"
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose가 설치되지 않았습니다."
        exit 1
    fi
    print_success "Docker Compose 설치됨"
    if ! curl -s "${APP_BASE_URL}/actuator/health" &> /dev/null; then
        print_warning "Spring Boot 애플리케이션이 실행되지 않았습니다."
        echo ""
        echo "애플리케이션 실행:"
        echo "  ./gradlew bootRun"
        echo ""
        read -p "계속하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        print_success "애플리케이션 실행 중 (${APP_BASE_URL})"
    fi
}

start_monitoring() {
    print_header "성능 모니터링 환경 시작 중..."

    cd "${INFRA_DIR}"

    docker-compose -f docker-compose.performance.yml up -d

    print_info "InfluxDB 초기화 대기 중..."
    sleep 5
    if docker ps | grep -q "eventitta-influxdb"; then
        print_success "InfluxDB 실행 중 (${INFLUX_URL})"
    else
        print_error "InfluxDB 시작 실패"
        docker-compose -f docker-compose.performance.yml logs influxdb || true
        exit 1
    fi

    if docker ps | grep -q "eventitta-grafana"; then
        print_success "Grafana 실행 중 (${GRAFANA_URL})"
        print_info "  로그인: admin / admin"
    else
        print_error "Grafana 시작 실패"
        docker-compose -f docker-compose.performance.yml logs grafana || true
        exit 1
    fi

    cd "${PROJECT_ROOT}"
}

run_test() {
    normalize_urls
    print_header "성능 테스트 실행 중..."

    cd "${PROJECT_ROOT}"
    local CACHE_TAG
    if [ -n "$1" ]; then
        CACHE_TAG="$1"
    else
        CACHE_TAG="$CACHE_TAG_DEFAULT"
    fi

    print_info "테스트 타입: cache=${CACHE_TAG}"
    print_info "대상: ${APP_BASE_URL}"
    print_info "Influx 출력: ${INFLUX_URL}"
    print_info "예상 소요 시간: 약 7분 30초"
    echo ""

    k6 run \
        --out "influxdb=${INFLUX_URL}" \
        --tag "cache=${CACHE_TAG}" \
        --tag "test_date=$(date +%Y-%m-%d_%H-%M-%S)" \
        -e "BASE_URL=${APP_BASE_URL}" \
        "${SCRIPT_DIR}/region-baseline.js"

    RESULT=$?

    if [ $RESULT -eq 0 ]; then
        echo ""
        print_success "테스트 완료!"
        echo ""
        print_info "Grafana에서 결과 확인:"
        print_info "  ${GRAFANA_URL}"
        echo ""
    else
        print_error "테스트 실패 (exit code: $RESULT)"
        exit $RESULT
    fi
}

stop_monitoring() {
    print_header "성능 모니터링 환경 종료 중..."

    cd "${INFRA_DIR}"
    docker-compose -f docker-compose.performance.yml stop

    print_success "환경 종료 완료"

    cd "${PROJECT_ROOT}"
}

clean_all() {
    print_header "데이터 삭제 중..."

    cd "${INFRA_DIR}"

    print_warning "모든 테스트 데이터가 삭제됩니다."
    read -p "계속하시겠습니까? (y/N): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose -f docker-compose.performance.yml down -v
        print_success "데이터 삭제 완료"
    else
        print_info "취소됨"
    fi

    cd "${PROJECT_ROOT}"
}

show_help() {
    cat << EOF
Region API 성능 테스트 도구

사용법:
  $0 [명령어] [옵션] [태그]

명령어:
  start                   성능 모니터링 환경 시작 (InfluxDB + Grafana)
  test [true|false]       성능 테스트 실행 (기본 tag: cache=false)
  stop                    성능 모니터링 환경 종료
  clean                   데이터 삭제 (볼륨 포함)
  all [true|false]        전체 실행 (start + test)

공통 옵션:
  --base-url <url>        대상 애플리케이션 BASE URL (기본: ${DEFAULT_APP_URL})
  --influx-url <url>      InfluxDB 수집 URL (기본: ${DEFAULT_INFLUX_URL})
  --grafana-url <url>     Grafana 접속 URL (기본: ${DEFAULT_GRAFANA_URL})

환경변수(옵션보다 낮은 우선순위):
  BASE_URL 또는 TARGET_BASE_URL
  PERF_INFLUX_URL
  PERF_GRAFANA_URL

예시:
  # 캐싱 전 테스트(기본값 사용)
  $0 all

  # 환경별 URL 지정
  $0 test --base-url https://dev.api.example.com --influx-url http://localhost:8086/k6

  # 캐싱 후 테스트 (캐싱 적용 후 실행)
  $0 test true --base-url http://localhost:8081

접속 URL(기본값):
  - Grafana: ${DEFAULT_GRAFANA_URL} (admin/admin)
  - InfluxDB: ${DEFAULT_INFLUX_URL}
  - 애플리케이션: ${DEFAULT_APP_URL}

EOF
}

case "${CMD}" in
    start)
        check_requirements
        start_monitoring
        ;;
    test)
        check_requirements
        run_test "${CACHE_TAG_DEFAULT}"
        ;;
    stop)
        stop_monitoring
        ;;
    clean)
        clean_all
        ;;
    all)
        check_requirements
        start_monitoring
        run_test "${CACHE_TAG_DEFAULT}"
        echo ""
        print_info "환경을 종료하려면: $0 stop"
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "알 수 없는 명령어: ${CMD}"
        echo ""
        show_help
        exit 1
        ;;
esac
