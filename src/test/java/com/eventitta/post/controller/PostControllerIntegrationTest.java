package com.eventitta.post.controller;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.WithMockCustomUser;
import com.eventitta.post.domain.Post;
import com.eventitta.post.dto.request.CreatePostRequest;
import com.eventitta.post.dto.request.UpdatePostRequest;
import com.eventitta.post.repository.PostRepository;
import com.eventitta.region.domain.Region;
import com.eventitta.region.repository.RegionRepository;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static com.eventitta.common.constants.ValidationMessage.*;
import static com.eventitta.common.exception.CommonErrorCode.INVALID_INPUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("동네 기반 게시글 컨트롤러 통합 테스트")
public class PostControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private RegionRepository regionRepository;
    private Long testUserId;

    @BeforeEach
    void createTestUserAndRegion() {
        testUserId = userRepository.save(
            User.builder()
                .email("it123123@test.com")
                .password("pw12312312312")
                .nickname("테스터111")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .build()
        ).getId();

        regionRepository.save(Region.builder()
            .code("1100110100")
            .name("서울특별시 종로구")
            .parentCode("1100000000")
            .level(3)
            .build()
        );
    }


    @Test
    @WithMockCustomUser(userId = 1L)
    @DisplayName("게시글을 생성하면 새 게시글의 식별자를 반환한다")
    void givenValidCreatePostRequest_whenCreatePost_thenReturnsNewPostId() throws Exception {
        CreatePostRequest req = new CreatePostRequest("제목", "내용", "1100110100");
        mockMvc.perform(post("/api/v1/posts")
                .cookie(buildAccessTokenCookie(testUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andDo(result -> {
                Number idValue = JsonPath.read(
                    result.getResponse().getContentAsString(),
                    "$.id"
                );
                Long newId = idValue.longValue();
                assertTrue(postRepository.existsById(newId));
            });
    }

    @Test
    @WithMockCustomUser(userId = 1L)
    @DisplayName("제목 없이 게시글을 생성하면 등록에 실패한다")
    void givenEmptyTitle_whenCreatePost_thenReturnsBadRequest() throws Exception {
        CreatePostRequest badReq = new CreatePostRequest(
            "",
            "내용",
            "1100110100"
        );

        mockMvc.perform(post("/api/v1/posts")
                .cookie(buildAccessTokenCookie(testUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("title: " + TITLE))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
        ;
    }

    @Test
    @WithMockCustomUser(userId = 1L)
    @DisplayName("내용 없이 게시글을 생성하면 등록에 실패한다")
    void givenEmptyContent_whenCreatePost_thenReturnsBadRequest() throws Exception {
        CreatePostRequest badReq = new CreatePostRequest(
            "테스트 제목",
            "",
            "1100110100"
        );

        mockMvc.perform(post("/api/v1/posts")
                .cookie(buildAccessTokenCookie(testUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("content: " + CONTENT))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @WithMockCustomUser(userId = 1L)
    @DisplayName("지역 코드를 지정하지 않고 게시글을 생성하면 등록에 실패한다")
    void givenEmptyRegion_whenCreatePost_thenReturnsBadRequest() throws Exception {
        CreatePostRequest badReq = new CreatePostRequest(
            "제목",
            "내용",
            ""
        );

        mockMvc.perform(post("/api/v1/posts")
                .cookie(buildAccessTokenCookie(testUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("regionCode: " + REGION_CODE))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @WithMockCustomUser(userId = 1L)
    @DisplayName("게시글 수정 내용은 저장소에 정상적으로 반영된다")
    void givenValidUpdateRequest_whenUpdatePost_thenReflectChanges() throws Exception {
        // given
        Post post = postRepository.save(Post.create(
            userRepository.findById(testUserId).get(),
            "old title", "old content",
            regionRepository.findById("1100110100").get()
        ));
        Long postId = post.getId();

        // when
        UpdatePostRequest req = new UpdatePostRequest("new title", "new content", "1100110100");
        mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                .cookie(buildAccessTokenCookie(testUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isNoContent());

        // then
        Post updated = postRepository.findById(postId).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("new title");
        assertThat(updated.getContent()).isEqualTo("new content");
    }

    @Test
    @WithMockCustomUser(userId = 1L)
    @DisplayName("게시글 제목 없이는 수정할 수 없다")
    void givenEmptyTitle_whenUpdatePost_thenBadRequest() throws Exception {
        // given
        Post post = postRepository.save(Post.create(
            userRepository.findById(testUserId).get(),
            "old", "old",
            regionRepository.findById("1100110100").get()
        ));
        Long postId = post.getId();

        // when & then
        var badReq = new UpdatePostRequest("", "content", "1100110100");
        mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                .cookie(buildAccessTokenCookie(testUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockCustomUser(userId = 1L)
    @DisplayName("존재하지 않는 지역으로는 게시글을 수정할 수 없다")
    void givenNonexistentRegion_whenUpdatePost_thenNotFound() throws Exception {
        // given
        Post post = postRepository.save(Post.create(
            userRepository.findById(testUserId).get(),
            "old", "old",
            regionRepository.findById("1100110100").get()
        ));
        Long postId = post.getId();

        // when & then
        var badReq = new UpdatePostRequest("title", "content", "0000000000");
        mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                .cookie(buildAccessTokenCookie(testUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockCustomUser(userId = 2L)
    @DisplayName("작성자만 게시글을 수정할 수 있다")
    void givenDifferentUser_whenUpdatePost_thenForbidden() throws Exception {
        // given
        Post post = postRepository.save(Post.create(
            userRepository.findById(testUserId).get(),
            "old", "old",
            regionRepository.findById("1100110100").get()
        ));
        Long postId = post.getId();

        // when & then
        UpdatePostRequest req = new UpdatePostRequest("title", "content", "1100110100");
        mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                .cookie(buildAccessTokenCookie(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockCustomUser(userId = 1L)
    @DisplayName("존재하지 않는 게시글은 수정할 수 없다")
    void givenNotExistPost_whenUpdatePost_thenNotFound() throws Exception {
        // when & then
        var req = new UpdatePostRequest("title", "content", "1100110100");
        mockMvc.perform(put("/api/v1/posts/{postId}", 9999L)
                .cookie(buildAccessTokenCookie(testUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isNotFound());
    }
}
