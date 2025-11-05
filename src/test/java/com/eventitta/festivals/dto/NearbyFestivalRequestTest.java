package com.eventitta.festivals.dto;

import com.eventitta.festivals.dto.request.NearbyFestivalRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("내 주변 축제 검색 조건 테스트")
class NearbyFestivalRequestTest {

    @Test
    @DisplayName("페이지 번호 자동 조정 - 페이지 번호를 입력하지 않으면 첫 번째 페이지로 설정된다")
    void givenNullPage_whenCreateRequest_thenPageIsSetToDefault() {
        // given
        Integer nullPage = null;
        Integer validSize = 10;

        // when
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0, null, null, nullPage, validSize
        );

        // then
        assertThat(request.page()).isEqualTo(0);
    }

    @Test
    @DisplayName("페이지 번호 자동 조정 - 페이지 번호가 음수이면 첫 번째 페이지로 설정된다")
    void givenNegativePage_whenCreateRequest_thenPageIsSetToDefault() {
        // given
        Integer negativePage = -1;
        Integer validSize = 10;

        // when
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0, null, null, negativePage, validSize
        );

        // then
        assertThat(request.page()).isEqualTo(0);
    }

    @Test
    @DisplayName("페이지 수 자동 조정 - 페이지 개수를 입력하지 않으면 기본값(20개)으로 설정된다")
    void givenNullSize_whenCreateRequest_thenSizeIsSetToDefault() {
        // given
        Integer validPage = 0;
        Integer nullSize = null;

        // when
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0, null, null, validPage, nullSize
        );

        // then
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("페이지 개수 자동 조정 - 페이지 개수가 너무 적으면(1개 미만) 기본값(20개)으로 설정된다")
    void givenTooSmallSize_whenCreateRequest_thenSizeIsSetToDefault() {
        // given
        Integer validPage = 0;
        Integer tooSmallSize = 0;

        // when
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0, null, null, validPage, tooSmallSize
        );

        // then
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("페이지 개수 자동 조정 - 페이지 개수가 너무 많으면(100개 초과) 기본값(20개)으로 설정된다")
    void givenTooLargeSize_whenCreateRequest_thenSizeIsSetToDefault() {
        // given
        Integer validPage = 0;
        Integer tooLargeSize = 101;

        // when
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0, null, null, validPage, tooLargeSize
        );

        // then
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("검색 시작일 설정 - 시작일을 입력하면 해당 날짜부터 검색한다")
    void givenStartDate_whenGetStartDateTime_thenReturnsStartOfDay() {
        // given
        LocalDate from = LocalDate.of(2025, 8, 15);
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0, from, null, 0, 20
        );

        // when
        LocalDateTime result = request.getStartDateTime();

        // then
        LocalDateTime expected = from.atStartOfDay();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("검색 시작일 기본값 - 시작일을 입력하지 않으면 오늘부터 검색한다")
    void givenNullStartDate_whenGetStartDateTime_thenReturnsTodayStartOfDay() {
        // given
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0, null, null, 0, 20
        );

        // when
        LocalDateTime result = request.getStartDateTime();

        // then
        LocalDateTime expected = LocalDate.now().atStartOfDay();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("검색 종료일 설정 - 종료일을 입력하면 해당 날짜까지 검색한다")
    void givenEndDate_whenGetEndDateTime_thenReturnsEndOfDay() {
        // given
        LocalDate to = LocalDate.of(2025, 8, 31);
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0, null, to, 0, 20
        );

        // when
        LocalDateTime result = request.getEndDateTime();

        // then
        LocalDateTime expected = to.atTime(LocalTime.MAX);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("검색 종료일 기본값 - 종료일을 입력하지 않으면 무제한으로 검색한다")
    void givenNullEndDate_whenGetEndDateTime_thenReturnsMaxDateTime() {
        // given
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0, null, null, 0, 20
        );

        // when
        LocalDateTime result = request.getEndDateTime();

        // then
        LocalDateTime expected = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("검색 조건 설정 - 모든 조건을 정확히 입력하면 그대로 적용된다")
    void givenAllValidParameters_whenCreateRequest_thenAllFieldsAreSetCorrectly() {
        // given
        Double latitude = 37.5665;
        Double longitude = 126.9780;
        Double distanceKm = 10.0;
        LocalDate from = LocalDate.of(2025, 8, 1);
        LocalDate to = LocalDate.of(2025, 8, 31);
        Integer page = 1;
        Integer size = 50;

        // when
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                latitude, longitude, distanceKm, from, to, page, size
        );

        // then
        assertThat(request.latitude()).isEqualTo(latitude);
        assertThat(request.longitude()).isEqualTo(longitude);
        assertThat(request.distanceKm()).isEqualTo(distanceKm);
        assertThat(request.from()).isEqualTo(from);
        assertThat(request.to()).isEqualTo(to);
        assertThat(request.page()).isEqualTo(page);
        assertThat(request.size()).isEqualTo(size);
    }

    @Test
    @DisplayName("검색 조건 확인 - 입력한 조건을 다시 확인할 수 있다")
    void givenRequest_whenAccessFields_thenReturnsCorrectValues() {
        // given
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0,
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2025, 8, 31),
                1, 20
        );

        // when & then
        assertThat(request.latitude()).isEqualTo(37.5665);
        assertThat(request.longitude()).isEqualTo(126.9780);
        assertThat(request.distanceKm()).isEqualTo(5.0);
        assertThat(request.from()).isEqualTo(LocalDate.of(2025, 8, 1));
        assertThat(request.to()).isEqualTo(LocalDate.of(2025, 8, 31));
        assertThat(request.page()).isEqualTo(1);
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("날짜 범위 확인 - 시작일과 종료일을 모두 입력했을 때 올바른 기간으로 설정된다")
    void givenBothDates_whenGetDateTimeRange_thenReturnsCorrectRange() {
        // given
        LocalDate from = LocalDate.of(2025, 8, 1);
        LocalDate to = LocalDate.of(2025, 8, 31);
        NearbyFestivalRequest request = new NearbyFestivalRequest(
                37.5665, 126.9780, 5.0, from, to, 0, 20
        );

        // when
        LocalDateTime startDateTime = request.getStartDateTime();
        LocalDateTime endDateTime = request.getEndDateTime();

        // then
        assertThat(startDateTime).isEqualTo(from.atStartOfDay());
        assertThat(endDateTime).isEqualTo(to.atTime(LocalTime.MAX));
        assertThat(startDateTime).isBefore(endDateTime);
    }
}
