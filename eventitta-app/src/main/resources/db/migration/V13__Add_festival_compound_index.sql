-- Festival 거리 검색 최적화를 위한 복합 인덱스 추가
-- 날짜 + 위치 조합으로 검색 성능 향상

-- Bounding Box 필터링과 날짜 조건을 활용한 복합 인덱스
CREATE INDEX idx_date_location ON festivals(start_date, latitude, longitude);
