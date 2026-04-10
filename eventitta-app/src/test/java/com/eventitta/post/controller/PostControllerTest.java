package com.eventitta.api.post.controller;

import com.eventitta.ControllerTestSupport;
import com.eventitta.WithMockCustomUser;
import com.eventitta.api.auth.domain.UserPrincipal;
import com.eventitta.domain.common.constants.ValidationMessage;
import com.eventitta.domain.common.response.PageResponse;
import com.eventitta.domain.post.domain.Post;
import com.eventitta.domain.post.dto.PostFilter;
import com.eventitta.domain.post.dto.request.CreatePostRequest;
import com.eventitta.domain.post.dto.request.UpdatePostRequest;
import com.eventitta.domain.post.dto.response.CreatePostResponse;
import com.eventitta.domain.post.dto.response.PostDetailResponse;
import com.eventitta.domain.post.dto.response.PostLikeStateResponse;
import com.eventitta.domain.post.dto.response.PostSummaryResponse;
import com.eventitta.domain.post.exception.PostErrorCode;
import com.eventitta.domain.post.exception.PostException;
import com.eventitta.domain.region.domain.Region;
import com.eventitta.domain.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static com.eventitta.domain.common.constants.ValidationMessage.*;
import static com.eventitta.domain.common.exception.CommonErrorCode.INVALID_INPUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
            .willReturn(new CreatePostResponse(fakePostId)); // 수정: 파라미터 개수 변경

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
    @DisplayName("게시글 이미지가 5개를 초과하면 요청이 거부된다")
    void givenTooManyImageMediaIds_whenCreate_thenBadRequest() throws Exception {
        CreatePostRequest bad = new CreatePostRequest(
            "test title!",
            "test content!",
            "1100110100",
            List.of(1L, 2L, 3L, 4L, 5L, 6L)
        );

        mockMvc.perform(post("/api/v1/posts")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()))
            .andExpect(jsonPath("$.message").value("imageMediaIds: 게시글 이미지는 최대 5개까지 첨부할 수 있습니다."));
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("알 수 없는 요청 필드가 포함되면 400으로 거부된다")
    void givenUnknownField_whenCreate_thenBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "title": "test title!",
                      "content": "test content!",
                      "regionCode": "1100110100",
                      "imageUrls": ["1", "2"]
                    }
                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()))
            .andExpect(jsonPath("$.message").value("알 수 없는 요청 필드입니다: imageUrls"));
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
        PageResponse<PostSummaryResponse> dummy = new PageResponse<>(
            List.of(),
            0,
            10,
            0,
            0
        );

        given(postService.getPosts(isNull(), any(PostFilter.class)))
            .willReturn(dummy);

        // when & then
        mockMvc.perform(get("/api/v1/posts")
                .param("page", "0")
                .param("size", "10")
            )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 사용자는 게시글 목록 조회 시 likedByMe 응답을 받을 수 있다.")
    void authenticatedUser_whenGetPosts_thenPassesPrincipalAndReturnsLikedByMe() throws Exception {
        // given
        PostSummaryResponse summary = new PostSummaryResponse(
            1L,
            "첫 글",
            "nick1",
            "1100110100",
            3,
            true,
            LocalDateTime.now()
        );
        PageResponse<PostSummaryResponse> dummy = new PageResponse<>(List.of(summary), 0, 10, 1, 1);
        given(postService.getPosts(any(), any(PostFilter.class))).willReturn(dummy);

        // when & then
        authenticateAs(42L);
        try {
            mockMvc.perform(get("/api/v1/posts")
                    .param("page", "0")
                    .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].likedByMe").value(true));

            verify(postService).getPosts(eq(42L), any(PostFilter.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
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
        PostDetailResponse dummy = new PostDetailResponse(
            postId,
            "title",
            "content",
            "nickname",
            "authProfileUrl",
            42L,
            "1100110101",
            1,
            false,
            10,
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        given(postService.getPost(eq(postId), isNull()))
            .willReturn(dummy);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}", postId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(postId))
            .andExpect(jsonPath("$.title").value(dummy.title()))
            .andExpect(jsonPath("$.content").value(dummy.content()))
            .andExpect(jsonPath("$.authorNickname").value(dummy.authorNickname()))
            .andExpect(jsonPath("$.authorProfileUrl").value(dummy.authorProfileUrl()))
            .andExpect(jsonPath("$.authorId").value(dummy.authorId()))
            .andExpect(jsonPath("$.likeCount").value(dummy.likeCount()))
            .andExpect(jsonPath("$.likedByMe").value(false))
            .andExpect(jsonPath("$.regionCode").value(dummy.regionCode()));
    }

    @Test
    @DisplayName("로그인 사용자는 게시글 상세 조회 시 likedByMe 응답을 받을 수 있다.")
    void authenticatedUser_whenGetPost_thenPassesPrincipalAndReturnsLikedByMe() throws Exception {
        // given
        long postId = 1L;
        PostDetailResponse dummy = new PostDetailResponse(
            postId,
            "title",
            "content",
            "nickname",
            "authProfileUrl",
            42L,
            "1100110101",
            2,
            true,
            10,
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        given(postService.getPost(any(Long.class), any())).willReturn(dummy);

        // when & then
        authenticateAs(42L);
        try {
            mockMvc.perform(get("/api/v1/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByMe").value(true));

            verify(postService).getPost(postId, 42L);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("유저는 존재하는 게시글에 대해 좋아요를 할 수 있다.")
    void likePost_shouldReturnState() throws Exception {
        // given
        Long postId = 100L;
        PostLikeStateResponse response = PostLikeStateResponse.liked(postId, 3);
        given(postService.likePost(postId, 42L)).willReturn(response);

        // when & then
        mockMvc.perform(
                put("/api/v1/posts/{postId}/like", postId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.postId").value(postId))
            .andExpect(jsonPath("$.likedByMe").value(true))
            .andExpect(jsonPath("$.likeCount").value(3));

        verify(postService).likePost(postId, 42L);
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("유저는 존재하는 게시글에 대해 좋아요를 취소할 수 있다.")
    void unlikePost_shouldReturnState() throws Exception {
        // given
        Long postId = 100L;
        PostLikeStateResponse response = PostLikeStateResponse.unliked(postId, 2);
        given(postService.unlikePost(postId, 42L)).willReturn(response);

        // when & then
        mockMvc.perform(delete("/api/v1/posts/{postId}/like", postId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.postId").value(postId))
            .andExpect(jsonPath("$.likedByMe").value(false))
            .andExpect(jsonPath("$.likeCount").value(2));

        verify(postService).unlikePost(postId, 42L);
    }

    @Test
    @WithMockCustomUser(userId = 42L)
    @DisplayName("본인이 추천한 게시글 목록을 페이지로 조회할 수 있다.")
    void getLikedPosts_shouldReturnPagedList() throws Exception {
        // given
        PostSummaryResponse s1 = new PostSummaryResponse(1L, "첫 글", "nick1", "1100110100", 3, true, LocalDateTime.now());
        PostSummaryResponse s2 = new PostSummaryResponse(2L, "두 번째 글", "nick2", "1100110100", 5, true, LocalDateTime.now());
        PageResponse<PostSummaryResponse> page = new PageResponse<>(List.of(s1, s2), 0, 10, 2, 1);
        given(postService.getLikedPosts(eq(42L), any(PostFilter.class))).willReturn(page);

        // when
        mockMvc.perform(
                get("/api/v1/posts/liked")
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].likedByMe").value(true));
    }

    private UserPrincipal testUserPrincipal(Long userId) {
        return new UserPrincipal(userId, "test@example.com", "USER");
    }

    private void authenticateAs(Long userId) {
        UserPrincipal principal = testUserPrincipal(userId);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        ));
        SecurityContextHolder.setContext(context);
    }
}
