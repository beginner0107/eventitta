package com.eventitta.festivals.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BoundingBox 계산 유틸리티 테스트")
class BoundingBoxCalculatorTest {

    private final BoundingBoxCalculator calculator = new BoundingBoxCalculator();

    @Test
    @DisplayName("서울 중심 10km Bounding Box 계산")
    void givenSeoulCenterAnd10Km_whenCalculate_thenReturnsBoundingBox() {
        // given
        double latitude = 37.5665;   // 서울 위도
        double longitude = 126.9780; // 서울 경도
        double distanceKm = 10.0;

        // when
        BoundingBox box = calculator.calculate(latitude, longitude, distanceKm);

        // then
        assertThat(box.minLatitude()).isCloseTo(37.4765, org.assertj.core.data.Offset.offset(0.001));
        assertThat(box.maxLatitude()).isCloseTo(37.6565, org.assertj.core.data.Offset.offset(0.001));
        assertThat(box.minLongitude()).isCloseTo(126.865, org.assertj.core.data.Offset.offset(0.001));
        assertThat(box.maxLongitude()).isCloseTo(127.091, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("작은 반경(1km)으로 Bounding Box 계산")
    void givenSmallRadius_whenCalculate_thenReturnsSmallBoundingBox() {
        // given
        double latitude = 37.5665;
        double longitude = 126.9780;
        double distanceKm = 1.0;

        // when
        BoundingBox box = calculator.calculate(latitude, longitude, distanceKm);

        // then
        double latDiff = box.maxLatitude() - box.minLatitude();
        double lonDiff = box.maxLongitude() - box.minLongitude();

        assertThat(latDiff).isCloseTo(0.018, org.assertj.core.data.Offset.offset(0.001)); // 약 0.018도
        assertThat(lonDiff).isCloseTo(0.023, org.assertj.core.data.Offset.offset(0.001)); // 약 0.023도
    }

    @Test
    @DisplayName("큰 반경(50km)으로 Bounding Box 계산")
    void givenLargeRadius_whenCalculate_thenReturnsLargeBoundingBox() {
        // given
        double latitude = 37.5665;
        double longitude = 126.9780;
        double distanceKm = 50.0;

        // when
        BoundingBox box = calculator.calculate(latitude, longitude, distanceKm);

        // then
        double latDiff = box.maxLatitude() - box.minLatitude();
        double lonDiff = box.maxLongitude() - box.minLongitude();

        // 50km 반경이므로 위도 차이는 약 0.9도, 경도 차이는 약 1.13도
        assertThat(latDiff).isCloseTo(0.9, org.assertj.core.data.Offset.offset(0.01));
        assertThat(lonDiff).isCloseTo(1.13, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("적도(위도 0)에서 Bounding Box 계산")
    void givenEquator_whenCalculate_thenReturnsBoundingBox() {
        // given
        double latitude = 0.0;  // 적도
        double longitude = 0.0;
        double distanceKm = 10.0;

        // when
        BoundingBox box = calculator.calculate(latitude, longitude, distanceKm);

        // then
        // 적도에서는 위도 1도 = 경도 1도 = 약 111km
        double latDiff = box.maxLatitude() - box.minLatitude();
        double lonDiff = box.maxLongitude() - box.minLongitude();

        // 위도와 경도 차이가 거의 동일해야 함
        assertThat(latDiff).isCloseTo(lonDiff, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("고위도(위도 60)에서 Bounding Box 계산")
    void givenHighLatitude_whenCalculate_thenReturnsWiderLongitudeRange() {
        // given
        double latitude = 60.0;  // 고위도
        double longitude = 0.0;
        double distanceKm = 10.0;

        // when
        BoundingBox box = calculator.calculate(latitude, longitude, distanceKm);

        // then
        double latDiff = box.maxLatitude() - box.minLatitude();
        double lonDiff = box.maxLongitude() - box.minLongitude();

        // 고위도에서는 경도 차이가 위도 차이보다 커야 함
        assertThat(lonDiff).isGreaterThan(latDiff);
    }

    @Test
    @DisplayName("음수 위도(남반구)에서 Bounding Box 계산")
    void givenSouthernHemisphere_whenCalculate_thenReturnsBoundingBox() {
        // given
        double latitude = -33.8688;   // 시드니 위도
        double longitude = 151.2093;  // 시드니 경도
        double distanceKm = 10.0;

        // when
        BoundingBox box = calculator.calculate(latitude, longitude, distanceKm);

        // then
        assertThat(box.minLatitude()).isLessThan(latitude);
        assertThat(box.maxLatitude()).isGreaterThan(latitude);
        assertThat(box.minLongitude()).isLessThan(longitude);
        assertThat(box.maxLongitude()).isGreaterThan(longitude);

        // 중심이 Bounding Box 안에 있어야 함
        assertThat(latitude).isBetween(box.minLatitude(), box.maxLatitude());
        assertThat(longitude).isBetween(box.minLongitude(), box.maxLongitude());
    }
}
