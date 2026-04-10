import json
import os
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse


def env_int(name: str, default: int) -> int:
    raw = os.getenv(name)
    if raw is None or raw == "":
        return default
    return int(raw)


def dataset_status(name: str, default: int = 200) -> int:
    return env_int(f"MOCK_{name}_STATUS", default)


def dataset_delay(name: str) -> int:
    return env_int(f"MOCK_{name}_DELAY_MS", 0)


class MockHandler(BaseHTTPRequestHandler):
    server_version = "eventitta-mock/1.0"

    def do_GET(self):
        parsed = urlparse(self.path)

        if parsed.path == "/health":
            self.respond_json(200, {"status": "UP"})
            return

        if parsed.path == "/national":
            self.respond_dataset("NATIONAL", self.national_payload(parsed))
            return

        if parsed.path.startswith("/seoul/"):
            self.respond_dataset("SEOUL", self.seoul_payload(parsed))
            return

        if parsed.path == "/geocoding/search":
            self.respond_dataset("GEOCODING", self.geocoding_payload(parsed))
            return

        if parsed.path.startswith("/kakao/"):
            self.respond_json(200, {"message": "mock kakao endpoint"})
            return

        self.respond_json(404, {"message": f"Unsupported mock path: {parsed.path}"})

    def log_message(self, fmt, *args):
        print(f"[mock-services] {self.address_string()} - {fmt % args}")

    def respond_dataset(self, name: str, payload):
        delay_ms = dataset_delay(name)
        if delay_ms > 0:
            time.sleep(delay_ms / 1000.0)

        status = dataset_status(name)
        if status != 200:
            self.respond_json(status, {"message": f"{name.lower()} mock forced error", "status": status})
            return

        self.respond_json(200, payload)

    def respond_json(self, status: int, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def national_payload(self, parsed):
        query = parse_qs(parsed.query)
        page_no = int(query.get("pageNo", ["1"])[0])
        num_rows = int(query.get("numOfRows", ["100"])[0])
        return {
            "response": {
                "header": {
                    "resultCode": "00",
                    "resultMsg": "NORMAL_SERVICE",
                    "type": "JSON",
                },
                "body": {
                    "items": [
                        {
                            "fstvlNm": f"Perf National Festival {page_no}",
                            "opar": "문화 행사 설명",
                            "fstvlStartDate": "2026-03-01",
                            "fstvlEndDate": "2026-03-30",
                            "fstvlCo": "mock national payload",
                            "mnnstNm": "Eventitta Mock",
                            "auspcInsttNm": "Eventitta Mock",
                            "suprtInsttNm": "Eventitta Mock",
                            "phoneNumber": "02-1234-5678",
                            "homepageUrl": "https://mock.eventitta.local/national",
                            "relateInfo": "performance validation",
                            "rdnmadr": "서울특별시 중구 세종대로 110",
                            "lnmadr": "서울특별시 중구 태평로1가",
                            "latitude": "37.5665000",
                            "longitude": "126.9780000",
                            "referenceDate": "2026-03-27",
                            "insttCode": "MOCK001",
                            "insttNm": "Mock Institute",
                        }
                    ],
                    "totalCount": 1,
                    "numOfRows": num_rows,
                    "pageNo": page_no,
                },
            }
        }

    def seoul_payload(self, parsed):
        path_parts = parsed.path.strip("/").split("/")
        date_token = path_parts[-1] if path_parts else ""
        return {
            "culturalEventInfo": {
                "row": [
                    {
                        "CODENAME": "공연",
                        "GUNAME": "중구",
                        "TITLE": f"Perf Seoul Event {date_token or 'all'}",
                        "DATE": "2026-03-01~2026-03-30",
                        "PLACE": "서울광장",
                        "ORG_NAME": "Eventitta Mock",
                        "USE_TRGT": "전체 이용가",
                        "USE_FEE": "무료",
                        "PLAYER": "Mock Team",
                        "PROGRAM": "performance validation",
                        "ETC_DESC": "mock seoul payload",
                        "ORG_LINK": "https://mock.eventitta.local/seoul",
                        "MAIN_IMG": "https://mock.eventitta.local/assets/seoul.png",
                        "RGSTDATE": "2026-03-27 09:00:00",
                        "TICKET": "현장 발권",
                        "STRTDATE": "2026-03-01 10:00:00",
                        "END_DATE": "2026-03-30 21:00:00",
                        "THEMECODE": "문화",
                        "LOT": "126.9780000",
                        "LAT": "37.5665000",
                        "IS_FREE": "Y",
                        "HMPG_ADDR": "https://mock.eventitta.local/seoul",
                    }
                ]
            }
        }

    def geocoding_payload(self, parsed):
        query = parse_qs(parsed.query)
        keyword = query.get("q", [""])[0]
        if not keyword.strip():
            return []
        return [
            {
                "lat": "37.5665000",
                "lon": "126.9780000",
                "display_name": f"Mock geocoding result for {keyword}",
            }
        ]


def main():
    bind = os.getenv("MOCK_BIND", "0.0.0.0")
    port = env_int("MOCK_PORT", 8080)
    server = ThreadingHTTPServer((bind, port), MockHandler)
    print(f"[mock-services] listening on {bind}:{port}")
    server.serve_forever()


if __name__ == "__main__":
    main()
