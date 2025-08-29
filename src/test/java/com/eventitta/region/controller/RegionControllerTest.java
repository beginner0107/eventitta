package com.eventitta.region.controller;

import com.eventitta.ControllerTestSupport;
import com.eventitta.region.dto.RegionDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegionControllerTest extends ControllerTestSupport {

    @Test
    void getTopRegions_returnsList() throws Exception {
        RegionDto r1 = new RegionDto("1100000000", "Seoul", 1);
        RegionDto r2 = new RegionDto("2600000000", "Busan", 1);
        when(regionService.getTopLevelRegions()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/regions").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(objectMapper.writeValueAsString(List.of(r1, r2))));
    }

    @Test
    void getTopRegions_returnsEmpty() throws Exception {
        when(regionService.getTopLevelRegions()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/regions").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void getChildRegions_returnsList() throws Exception {
        String parent = "1100000000";
        RegionDto c1 = new RegionDto("1100100000", "Jongno-gu", 2);
        RegionDto c2 = new RegionDto("1100200000", "Jung-gu", 2);
        when(regionService.getChildRegions(parent)).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/v1/regions/{parentCode}", parent).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(objectMapper.writeValueAsString(List.of(c1, c2))));
    }

    @Test
    void getChildRegions_returnsEmpty() throws Exception {
        String parent = "unknown";
        when(regionService.getChildRegions(parent)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/regions/{parentCode}", parent).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }
}

