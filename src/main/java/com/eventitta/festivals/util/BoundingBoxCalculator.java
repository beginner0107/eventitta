package com.eventitta.festivals.util;

import org.springframework.stereotype.Component;

/**
 * 중심 좌표와 반경을 기반으로 Bounding Box를 계산하는 유틸리티 클래스
 *
 * <p>Bounding Box는 거리 계산 전에 정사각형 영역으로 1차 필터링하여
 * 데이터베이스 검색 성능을 향상시키기 위해 사용됩니다.</p>
 *
 * <p>계산 공식:</p>
 * <ul>
 *   <li>위도 1도 = 약 111km (지구상 어디서나 일정)</li>
 *   <li>경도 1도 = 약 111km × cos(위도) (위도에 따라 변함)</li>
 * </ul>
 */
@Component
public class BoundingBoxCalculator {

    /**
     * 위도 1도당 거리 (km) - 지구상 어디서나 일정
     */
    private static final double KM_PER_DEGREE_LAT = 111.0;

    /**
     * 주어진 중심 좌표와 반경으로 Bounding Box 계산
     *
     * <p>이 메서드는 순수 계산 로직만 수행하며, 입력값 검증은 Service Layer에서 수행됩니다.</p>
     *
     * @param latitude   중심 위도 (-90 ~ 90)
     * @param longitude  중심 경도 (-180 ~ 180)
     * @param distanceKm 반경 (km, 양수)
     * @return 계산된 Bounding Box
     */
    public BoundingBox calculate(double latitude, double longitude, double distanceKm) {
        // 위도 변화량 계산
        double latDelta = distanceKm / KM_PER_DEGREE_LAT;

        // 경도 변화량 계산 (위도에 따라 달라짐)
        // cos(latitude)를 사용하여 위도에 따른 경도 거리 보정
        double lonDelta = distanceKm / (KM_PER_DEGREE_LAT * Math.cos(Math.toRadians(latitude)));

        return new BoundingBox(
            latitude - latDelta,   // minLatitude (남쪽 경계)
            latitude + latDelta,   // maxLatitude (북쪽 경계)
            longitude - lonDelta,  // minLongitude (서쪽 경계)
            longitude + lonDelta   // maxLongitude (동쪽 경계)
        );
    }
}
