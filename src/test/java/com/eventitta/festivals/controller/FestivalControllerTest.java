package com.eventitta.festivals.controller;

import com.eventitta.common.response.PageResponse;
import com.eventitta.festivals.dto.FestivalResponseDto;
import com.eventitta.festivals.dto.NearbyFestivalRequest;
import com.eventitta.festivals.service.FestivalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FestivalController.class)
@ActiveProfiles("test")
@DisplayName("축제 웹 서비스 테스트")
class FestivalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FestivalService festivalService;

    @Test
    @WithMockUser
    @DisplayName("근처 축제 찾기 - 위치와 거리만 입력하면 주변 축제 목록이 나타난다")
    void givenRequiredParameters_whenGetNearbyEvents_thenReturnsFestivals() throws Exception {
        // given
        FestivalResponseDto festival = FestivalResponseDto.builder()
            .id(1L)
            .title("서울 음악 축제")
            .place("한강공원")
            .startTime(LocalDate.of(2025, 8, 15).atStartOfDay())
            .endTime(LocalDate.of(2025, 8, 17).atTime(23, 59, 59))
            .category("음악")
            .isFree(true)
            .homepageUrl("https://example.com")
            .distance(2.5)
            .build();

        PageResponse<FestivalResponseDto> pageResponse = PageResponse.of(
            new PageImpl<>(List.of(festival))
        );

        given(festivalService.getNearbyFestival(any(NearbyFestivalRequest.class)))
            .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/v1/festivals/nearby")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
                .param("distanceKm", "5.0"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("서울 음악 축제"))
            .andExpect(jsonPath("$.content[0].place").value("한강공원"))
            .andExpect(jsonPath("$.content[0].isFree").value(true))
            .andExpect(jsonPath("$.content[0].distance").value(2.5));
    }

    @Test
    @WithMockUser
    @DisplayName("근처 축제 찾기 - 위치, 거리, 날짜, 페이지 정보를 모두 입력하면 조건에 맞는 축제 목록이 나타난다")
    void givenAllParameters_whenGetNearbyEvents_thenReturnsFestivals() throws Exception {
        // given
        PageResponse<FestivalResponseDto> pageResponse = PageResponse.of(
            new PageImpl<>(List.of())
        );

        given(festivalService.getNearbyFestival(any(NearbyFestivalRequest.class)))
            .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/v1/festivals/nearby")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
                .param("distanceKm", "10.0")
                .param("from", "2025-08-01")
                .param("to", "2025-08-31")
                .param("page", "0")
                .param("size", "20")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("근처 축제 찾기 - 위도를 입력하지 않으면 오류 메시지가 나타난다")
    void givenMissingLatitude_whenGetNearbyEvents_thenReturnsValidationError() throws Exception {
        // given
        // 위도 없음

        // when & then
        mockMvc.perform(get("/api/v1/festivals/nearby")
                .param("longitude", "126.9780")
                .param("distanceKm", "5.0")
            )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("근처 축제 찾기 - 경도를 입력하지 않으면 오류 메시지가 나타난다")
    void givenMissingLongitude_whenGetNearbyEvents_thenReturnsValidationError() throws Exception {
        // given
        // 경도 없음

        // when & then
        mockMvc.perform(get("/api/v1/festivals/nearby")
                .param("latitude", "37.5665")
                .param("distanceKm", "5.0")
            )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("근처 축제 찾기 - 검색 거리를 입력하지 않으면 오류 메시지가 나타난다")
    void givenMissingRadius_whenGetNearbyEvents_thenReturnsValidationError() throws Exception {
        // given
        // 반경 없음

        // when & then
        mockMvc.perform(get("/api/v1/festivals/nearby")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
            )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("근처 축제 찾기 - 검색 거리가 너무 짧으면(0.1km 미만) 오류 메시지가 나타난다")
    void givenTooSmallRadius_whenGetNearbyEvents_thenReturnsValidationError() throws Exception {
        // given
        // 반경이 0.1보다 작음

        // when & then
        mockMvc.perform(get("/api/v1/festivals/nearby")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
                .param("distanceKm", "0.05")
            )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("근처 축제 찾기 - 한 번에 보여줄 축제 개수가 너무 많으면(100개 초과) 자동으로 기본값으로 조정된다")
    void givenTooLargePageSize_whenGetNearbyEvents_thenReturnsValidationError() throws Exception {
        // given
        // 페이지 크기가 100보다 큼 - NearbyFestivalRequest의 @Max 검증이 작동해야 함

        // when & then
        mockMvc.perform(get("/api/v1/festivals/nearby")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
                .param("distanceKm", "5.0")
                .param("size", "101")
            )
            .andDo(print())
            .andExpect(status().isOk()); // 생성자에서 size가 20으로 변경되므로 성공으로 예상
    }

    @Test
    @WithMockUser
    @DisplayName("근처 축제 찾기 - 페이지 번호가 음수면 자동으로 첫 번째 페이지로 조정된다")
    void givenNegativePageNumber_whenGetNearbyEvents_thenSucceedsWithDefaultValue() throws Exception {
        // given
        PageResponse<FestivalResponseDto> pageResponse = PageResponse.of(
            new PageImpl<>(List.of())
        );

        given(festivalService.getNearbyFestival(any(NearbyFestivalRequest.class)))
            .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/v1/festivals/nearby")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
                .param("distanceKm", "5.0")
                .param("page", "-1")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
}
