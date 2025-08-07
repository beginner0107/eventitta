package com.eventitta.festivals.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("축제 정보 관리 테스트")
class FestivalTest {

    @Test
    @DisplayName("서울시 축제 생성 - 서울시 축제 정보를 생성할 수 있다")
    void givenSeoulFestivalData_whenCreateSeoulFestival_thenFestivalIsCreated() {
        // given & when
        Festival festival = Festival.createSeoulFestival(
                "한강여름축제",
                "한강공원",
                LocalDate.of(2025, 8, 15),
                LocalDate.of(2025, 8, 17),
                "음악",
                "강남구",
                "전연령",
                "무료",
                true,
                "아티스트A, 아티스트B",
                "음악 공연 및 체험 부스",
                "https://example.com/image.jpg",
                "MUSIC",
                "시민",
                "서울시",
                "https://example.com",
                "https://example.com/detail",
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                "축제 상세 내용",
                "seoul-ext-123"
        );

        // then
        assertThat(festival).isNotNull();
        assertThat(festival.getTitle()).isEqualTo("한강여름축제");
        assertThat(festival.getVenue()).isEqualTo("한강공원");
        assertThat(festival.getDataSource()).isEqualTo(DataSource.SEOUL_FESTIVAL);
        assertThat(festival.getCategory()).isEqualTo("음악");
        assertThat(festival.getDistrict()).isEqualTo("강남구");
        assertThat(festival.getIsFree()).isTrue();
    }

    @Test
    @DisplayName("전국축제 생성 - 전국축제 정보를 생성할 수 있다")
    void givenNationalFestivalData_whenCreateNationalFestival_thenFestivalIsCreated() {
        // given & when
        Festival festival = Festival.createNationalFestival(
                "부산바다축제",
                "해운대해수욕장",
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 3),
                "해변에서 즐기는 여름축제",
                "부산시",
                "https://busan-festival.com",
                new BigDecimal("35.1595"),
                new BigDecimal("129.1603"),
                "national-ext-456"
        );

        // then
        assertThat(festival).isNotNull();
        assertThat(festival.getTitle()).isEqualTo("부산바다축제");
        assertThat(festival.getVenue()).isEqualTo("해운대해수욕장");
        assertThat(festival.getDataSource()).isEqualTo(DataSource.NATIONAL_FESTIVAL);
        assertThat(festival.getOrganizer()).isEqualTo("부산시");
        assertThat(festival.getCategory()).isNull(); // 전국축제는 category가 없음
    }

    @Test
    @DisplayName("축제 정보 업데이트 - 기존 축제 정보를 새로운 정보로 업데이트할 수 있다")
    void givenSourceFestival_whenUpdateFestivalInfo_thenFieldsAreUpdated() {
        // given
        Festival originalFestival = Festival.createSeoulFestival(
                "원본 축제",
                "원본 장소",
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2025, 7, 3),
                "원본 카테고리",
                "서초구",
                "성인",
                "유료",
                false,
                "원본 출연자",
                "원본 프로그램",
                "https://original.com/image.jpg",
                "ORIGINAL",
                "기관",
                "원본 주최",
                "https://original.com",
                "https://original.com/detail",
                new BigDecimal("37.0000"),
                new BigDecimal("126.0000"),
                "원본 내용",
                "original-ext-123"
        );

        Festival updatedFestival = Festival.createSeoulFestival(
                "업데이트된 축제",
                "업데이트된 장소",
                LocalDate.of(2025, 8, 15),
                LocalDate.of(2025, 8, 17),
                "음악",
                "강남구",
                "전연령",
                "무료",
                true,
                "아티스트A, 아티스트B",
                "음악 공연 및 체험 부스",
                "https://example.com/image.jpg",
                "MUSIC",
                "시민",
                "서울시",
                "https://example.com",
                "https://example.com/detail",
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                "축제 상세 내용",
                "updated-ext-123"
        );

        // when
        originalFestival.updateFestivalInfo(updatedFestival);

        // then
        assertThat(originalFestival.getTitle()).isEqualTo("업데이트된 축제");
        assertThat(originalFestival.getVenue()).isEqualTo("업데이트된 장소");
        assertThat(originalFestival.getStartDate()).isEqualTo(LocalDate.of(2025, 8, 15));
        assertThat(originalFestival.getEndDate()).isEqualTo(LocalDate.of(2025, 8, 17));
        assertThat(originalFestival.getCategory()).isEqualTo("음악");
        assertThat(originalFestival.getDistrict()).isEqualTo("강남구");
        assertThat(originalFestival.getTargetAudience()).isEqualTo("전연령");
        assertThat(originalFestival.getFeeInfo()).isEqualTo("무료");
        assertThat(originalFestival.getIsFree()).isTrue();
        assertThat(originalFestival.getPerformers()).isEqualTo("아티스트A, 아티스트B");
        assertThat(originalFestival.getProgramInfo()).isEqualTo("음악 공연 및 체험 부스");
        assertThat(originalFestival.getMainImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(originalFestival.getThemeCode()).isEqualTo("MUSIC");
        assertThat(originalFestival.getTicketType()).isEqualTo("시민");
        assertThat(originalFestival.getOrganizer()).isEqualTo("서울시");
        assertThat(originalFestival.getHomepageUrl()).isEqualTo("https://example.com");
        assertThat(originalFestival.getDetailUrl()).isEqualTo("https://example.com/detail");
        assertThat(originalFestival.getLatitude()).isEqualTo(new BigDecimal("37.5665"));
        assertThat(originalFestival.getLongitude()).isEqualTo(new BigDecimal("126.9780"));
        assertThat(originalFestival.getContent()).isEqualTo("축제 상세 내용");
    }
}
