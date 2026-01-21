package com.eventitta.festivals.util;

/**
 * 위도/경도 기반 사각형 영역을 나타내는 DTO
 *
 * <p>검증은 Service Layer에서 수행하며, 이 DTO는 순수 데이터 전달 역할만 수행합니다.</p>
 *
 * @param minLatitude  최소 위도 (남쪽 경계)
 * @param maxLatitude  최대 위도 (북쪽 경계)
 * @param minLongitude 최소 경도 (서쪽 경계)
 * @param maxLongitude 최대 경도 (동쪽 경계)
 */
public record BoundingBox(
    double minLatitude,
    double maxLatitude,
    double minLongitude,
    double maxLongitude
) {}
