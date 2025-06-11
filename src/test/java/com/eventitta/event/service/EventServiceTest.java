package com.eventitta.event.service;

import com.eventitta.common.response.PageResponse;
import com.eventitta.event.dto.request.NearbyEventRequest;
import com.eventitta.event.dto.response.EventDistanceDto;
import com.eventitta.event.dto.response.EventResponseDto;
import com.eventitta.event.mapper.EventMapper;
import com.eventitta.event.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    EventMapper eventMapper;

    @InjectMocks
    EventService eventService;

    @Test
    @DisplayName("위치와 날짜 조건을 입력하면 주변 이벤트 목록을 가져온다")
    void getNearbyEvents_returnsPageResult() {
        // given
        NearbyEventRequest req = new NearbyEventRequest(37.5, 126.9, 5.0,
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), 0, 10);
        Pageable pageable = PageRequest.of(0, 10);
        Page<EventDistanceDto> page = new PageImpl<>(List.of(), pageable, 0);
        given(eventRepository.findEventsWithinDistanceAndDateBetween(
            eq(37.5), eq(126.9), eq(5.0), any(), any(), eq(pageable)
        )).willReturn(page);

        // when
        PageResponse<EventResponseDto> result = eventService.getNearbyEvents(req);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();

        verify(eventRepository).findEventsWithinDistanceAndDateBetween(
            eq(37.5), eq(126.9), eq(5.0), any(), any(), eq(pageable)
        );
    }
}
