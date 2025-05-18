package com.eventitta.post.controller;

import com.eventitta.ControllerTestSupport;
import com.eventitta.WithMockCustomUser;
import com.eventitta.post.dto.request.CreatePostRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.eventitta.common.constants.ValidationMessage.*;
import static com.eventitta.common.exception.CommonErrorCode.INVALID_INPUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("동네 기반 게시글 컨트롤러 슬라이스 테스트")
class PostControllerTest extends ControllerTestSupport {

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("게시글을 생성하면 새 게시글의 식별자를 반환한다")
    void givenValidCreatePostRequest_whenCreatePost_thenReturnsNewPostId() throws Exception {        // given
        long fakePostId = 100L;
        CreatePostRequest req = new CreatePostRequest("제목", "내용", "1100110100");

        given(postService.create(eq(42L), any(CreatePostRequest.class)))
            .willReturn(fakePostId);

        // when & then
        mockMvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/posts/" + fakePostId))
            .andExpect(jsonPath("$.id").value(fakePostId));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("제목 없이 게시글을 생성하면 등록에 실패한다.")
    void givenEmptyTitle_whenCreatePost_thenFailsValidation() throws Exception {
        // given
        CreatePostRequest bad = new CreatePostRequest(
            "",
            "C",
            "1100110100"
        );

        // when & then
        mockMvc.perform(post("/api/v1/posts")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("title: " + TITLE))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("내용 없이 게시글을 생성하면 등록에 실패한다.")
    void givenEmptyContent_whenCreatePost_thenFailsValidation() throws Exception {
        // given
        CreatePostRequest bad = new CreatePostRequest(
            "test title!",
            "",
            "1100110100"
        );

        // when & then
        mockMvc.perform(post("/api/v1/posts")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("content: " + CONTENT))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("지역 코드 없이 게시글을 생성하면 등록에 실패한다.")
    void givenEmptyRegion_whenCreatePost_thenFailsValidation() throws Exception {
        // given
        CreatePostRequest bad = new CreatePostRequest(
            "test title!",
            "test content!",
            ""
        );

        // when & then
        mockMvc.perform(post("/api/v1/posts")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("regionCode: " + REGION_CODE))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }
}
