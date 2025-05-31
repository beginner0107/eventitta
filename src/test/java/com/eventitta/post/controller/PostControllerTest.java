package com.eventitta.post.controller;

import com.eventitta.ControllerTestSupport;
import com.eventitta.WithMockCustomUser;
import com.eventitta.common.constants.ValidationMessage;
import com.eventitta.common.response.PageResponse;
import com.eventitta.post.domain.Post;
import com.eventitta.post.dto.PostFilter;
import com.eventitta.post.dto.request.CreatePostRequest;
import com.eventitta.post.dto.response.PostDetailDto;
import com.eventitta.post.dto.request.UpdatePostRequest;
import com.eventitta.post.dto.response.PostSummaryDto;
import com.eventitta.post.exception.PostErrorCode;
import com.eventitta.post.exception.PostException;
import com.eventitta.region.domain.Region;
import com.eventitta.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;

import static com.eventitta.common.constants.ValidationMessage.*;
import static com.eventitta.common.exception.CommonErrorCode.INVALID_INPUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("동네 기반 게시글 컨트롤러 슬라이스 테스트")
class PostControllerTest extends ControllerTestSupport {

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("게시글을 생성하면 새 게시글의 식별자를 반환한다")
    void givenValidCreatePostRequest_whenCreatePost_thenReturnsNewPostId() throws Exception {
        // given
        long fakePostId = 100L;
        CreatePostRequest req = new CreatePostRequest("제목", "내용", "1100110100", List.of());

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
            "1100110100",
            List.of()
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
            "1100110100",
            List.of()
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
            "",
            List.of()
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

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("올바른 데이터로 게시글 수정 시 성공적으로 수정된다.")
    void givenValidUpdateRequest_whenUpdate_thenNoContent() throws Exception {
        // given
        UpdatePostRequest req = new UpdatePostRequest("새 제목", "새 내용", "1100110100", List.of());
        doNothing().when(postService).update(eq(100L), eq(42L), any(UpdatePostRequest.class));

        // when & then
        mockMvc.perform(put("/api/v1/posts/" + 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("제목 없이 수정 시 게시글 수정에 실패한다.")
    void givenEmptyTitle_whenUpdate_thenBadRequest() throws Exception {
        UpdatePostRequest req = new UpdatePostRequest("", "내용", "1100110100", List.of());

        mockMvc.perform(put("/api/v1/posts/" + 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()))
            .andExpect(jsonPath("$.message").value("title: " + ValidationMessage.TITLE));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("제목 없이 수정 시 게시글 수정에 실패한다.")
    void givenEmptyContent_whenUpdate_thenBadRequest() throws Exception {
        UpdatePostRequest req = new UpdatePostRequest("testTitle", "", "1100110100", List.of());

        mockMvc.perform(put("/api/v1/posts/" + 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()))
            .andExpect(jsonPath("$.message").value("content: " + CONTENT));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("제목 없이 수정 시 게시글 수정에 실패한다.")
    void givenEmptyRegion_whenUpdate_thenBadRequest() throws Exception {
        UpdatePostRequest req = new UpdatePostRequest("testTitle", "testContent", "", List.of());

        mockMvc.perform(put("/api/v1/posts/" + 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()))
            .andExpect(jsonPath("$.message").value("regionCode: " + REGION_CODE));
    }


    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("작성자는 자신의 게시글을 삭제할 수 있다")
    void givenValidRequest_whenDelete_thenNoContent() throws Exception {
        // given
        long postId = 100L;
        doNothing().when(postService).delete(postId, 42L);

        // when & then
        mockMvc.perform(delete("/api/v1/posts/" + postId))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("존재하지 않는 게시글은 삭제할 수 없다")
    void givenNonexistentPost_whenDelete_thenNotFound() throws Exception {
        // given
        long postId = 101L;
        doThrow(new PostException(PostErrorCode.NOT_FOUND_POST_ID))
            .when(postService).delete(postId, 42L);

        // when & then
        mockMvc.perform(delete("/api/v1/posts/" + postId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value(PostErrorCode.NOT_FOUND_POST_ID.name()));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("작성자만 게시글을 삭제할 수 있다")
    void givenDifferentUser_whenDelete_thenForbidden() throws Exception {
        // given
        long postId = 102L;
        doThrow(new PostException(PostErrorCode.ACCESS_DENIED))
            .when(postService).delete(postId, 42L);

        // when & then
        mockMvc.perform(delete("/api/v1/posts/" + postId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(PostErrorCode.ACCESS_DENIED.name()));
    }

    @Test
    @DisplayName("모든 사용자는 게시글 목록을 조회할 수 있다.")
    void givenAnyUser_whenGetPosts_thenOk() throws Exception {
        // given
        PageResponse<PostSummaryDto> dummy = new PageResponse<>(
            List.of(),
            0,
            10,
            0,
            0
        );

        given(postService.getPosts(any(PostFilter.class)))
            .willReturn(dummy);

        // when & then
        mockMvc.perform(get("/api/v1/posts")
                .param("page", "0")
                .param("size", "10")
            )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("페이지 번호가 음수이면 요청이 거부된다")
    void givenNegativePageNumber_whenRequestingPosts_thenBadRequest() throws Exception {
        // given
        int negativePage = -1;

        // when & then
        mockMvc.perform(get("/api/v1/posts")
                .param("page", String.valueOf(negativePage))
                .param("size", "10")
                .param("searchType", "TITLE")
                .param("keyword", "foo")
                .param("regionCode", "1100110100")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()))
            .andExpect(jsonPath("$.message").value("page: " + PAGE_MIN));
    }

    @Test
    @DisplayName("페이지 크기가 허용된 범위를 벗어나면 요청이 거부된다")
    void givenOutOfRangePageSize_whenRequestingPosts_thenBadRequest() throws Exception {
        // given
        String zeroSize = "0";
        String badMaxSize = "101";

        // when & then
        for (String badSize : List.of(zeroSize, badMaxSize)) {
            mockMvc.perform(get("/api/v1/posts")
                    .param("page", "0")
                    .param("size", badSize)
                    .param("searchType", "TITLE")
                    .param("keyword", "foo")
                    .param("regionCode", "1100110100")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        }
    }

    @Test
    @DisplayName("모든 사용자는 게시글을 조회할 수 있다.")
    void givenAnyUser_whenGetPost_thenOk() throws Exception {
        // given
        long postId = 1L;
        PostDetailDto dummy = new PostDetailDto(
            postId,
            "title",
            "content",
            "nickname",
            "authProfileUrl",
            "1100110101",
            1,
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        given(postService.getPost(any(Long.class)))
            .willReturn(dummy);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}", postId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(postId))
            .andExpect(jsonPath("$.title").value(dummy.title()))
            .andExpect(jsonPath("$.content").value(dummy.content()))
            .andExpect(jsonPath("$.authorNickname").value(dummy.authorNickname()))
            .andExpect(jsonPath("$.authorProfileUrl").value(dummy.authorProfileUrl()))
            .andExpect(jsonPath("$.likeCount").value(dummy.likeCount()))
            .andExpect(jsonPath("$.regionCode").value(dummy.regionCode()));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("유저는 존재하는 게시글에 대해 추천을 할 수 잇다.")
    void likePost_shouldReturnNoContent() throws Exception {
        // given
        Long postId = 100L;

        // when & then
        mockMvc.perform(
                post("/api/v1/posts/{postId}/like", postId))
            .andExpect(status().isNoContent());

        verify(postService).toggleLike(postId, 42L);
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("본인이 추천한 게시글 목록 조회를 할 수 있다.")
    void getLikedPosts_shouldReturnList() throws Exception {
        // given
        List<Post> likedPosts = List.of(
            Post.builder().id(1L).title("첫 글").content("내용").user(mock(User.class)).region(mock(Region.class)).build(),
            Post.builder().id(2L).title("두 번째 글").content("내용2").user(mock(User.class)).region(mock(Region.class)).build()
        );
        given(postService.getLikedPosts(42L)).willReturn(likedPosts);

        // when
        mockMvc.perform(
                get("/api/v1/posts/liked"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
