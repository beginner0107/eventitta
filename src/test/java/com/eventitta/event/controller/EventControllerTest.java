package com.eventitta.event.controller;

import com.eventitta.common.response.PageResponse;
import com.eventitta.event.dto.request.NearbyEventRequest;
import com.eventitta.event.dto.response.EventResponseDto;
import com.eventitta.event.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    EventService eventService;

    @Test
    @DisplayName("필요한 정보를 모두 입력하면 주변 이벤트 목록을 성공적으로 가져온다")
    void whenValidRequest_thenReturnsPageResponse() throws Exception {
        EventResponseDto dto = Mockito.mock(EventResponseDto.class);
        PageResponse<EventResponseDto> pageResp = new PageResponse<>(
            List.of(dto), 0, 20, 1, 1
        );
        given(eventService.getNearbyEvents(any(NearbyEventRequest.class)))
            .willReturn(pageResp);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/events/nearby")
                .param("lat", "37.5665")
                .param("lng", "126.9780")
                .param("distanceKm", "5")
                .param("from", "2025-06-10")
                .param("to", "2025-06-20")
                .param("page", "0")
                .param("size", "20")
                .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("필수 정보를 빼먹고 요청하면 잘못된 요청이라는 오류가 발생한다")
    void whenMissingRequiredParams_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/events/nearby")
                .param("lng", "126.9780")
                .param("distanceKm", "5")
                .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest());
    }
}
