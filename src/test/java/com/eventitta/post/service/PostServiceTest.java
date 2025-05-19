package com.eventitta.post.service;

import com.eventitta.post.domain.Post;
import com.eventitta.post.dto.request.CreatePostRequest;
import com.eventitta.post.dto.request.UpdatePostRequest;
import com.eventitta.post.exception.PostErrorCode;
import com.eventitta.post.exception.PostException;
import com.eventitta.post.repository.PostRepository;
import com.eventitta.region.domain.Region;
import com.eventitta.region.exception.RegionErrorCode;
import com.eventitta.region.exception.RegionException;
import com.eventitta.region.repository.RegionRepository;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.exception.UserErrorCode;
import com.eventitta.user.exception.UserException;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@DisplayName("동네 기반 게시글 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    PostRepository postRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    RegionRepository regionRepository;

    @InjectMocks
    PostService postService;

    private final String VALID_REGION = "1100110100";
    private final String INVALID_REGION = "0000000000";

    @Test
    @DisplayName("유효한 요청으로 생성 시 새 게시글 ID가 반환된다")
    void whenValidRequest_thenReturnNewPostId() {
        // given
        long userId = 1L;
        String regionCode = "1100110100";
        CreatePostRequest createPostRequest = new CreatePostRequest("제목", "내용", regionCode);

        User user = createUser(userId, "test@test.com", "pw123123", "유저");
        Region region = createRegion(regionCode);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(regionRepository.findById(regionCode)).willReturn(Optional.of(region));

        Post savedPost = createPost(123L, user, createPostRequest.title(), region);
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        Long resultId = postService.create(userId, createPostRequest);

        // then
        assertThat(resultId).isEqualTo(123L);
    }


    @Test
    @DisplayName("존재하지 않는 사용자 ID로 생성 시 예외가 반환된다.")
    void whenUserNotFound_thenThrowUserNotFound() {
        // given
        long fakeUserId = 99L;
        CreatePostRequest dto = new CreatePostRequest("제목", "내용", "1100110100");
        given(userRepository.findById(fakeUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.create(fakeUserId, dto))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.NOT_FOUND_USER_ID);
    }

    @Test
    @DisplayName("존재하지 않는 지역 코드로 생성 시 예외가 반환된다.")
    void whenRegionNotFound_thenThrowRegionNotFound() {
        // given
        long fakeUserId = 1L;
        CreatePostRequest dto = new CreatePostRequest("제목", "내용", "0000000000");
        given(userRepository.findById(fakeUserId))
            .willReturn(Optional.of(User.builder().id(fakeUserId).build()));
        given(regionRepository.findById(dto.regionCode())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.create(fakeUserId, dto))
            .isInstanceOf(RegionException.class)
            .extracting("errorCode")
            .isEqualTo(RegionErrorCode.NOT_FOUND_REGION_CODE);
    }

    @Test
    @DisplayName("업데이트시 기존 게시물 및 소유자 및 유효한 지역이 주어지면 제목, 콘텐츠, 지역이 변경됩니다.")
    void givenPostAndOwnerAndValidRegion_whenUpdate_thenFieldsUpdated() {
        // given
        final long POST_ID = 1L;
        final long USER_ID = 10L;
        Region region = createRegion(VALID_REGION);
        Post post = spy(Post.create(
            createUser(USER_ID, "test@test.com", "pw123123", "유저"),
            "oldTitle", "oldContent",
            region
        ));
        given(postRepository.findByIdAndDeletedFalse(POST_ID))
            .willReturn(Optional.of(post));
        given(regionRepository.findById(VALID_REGION))
            .willReturn(Optional.of(region));

        UpdatePostRequest dto = createUpdateDto("newTitle", "newContent", VALID_REGION);

        // when
        postService.update(POST_ID, USER_ID, dto);

        // then
        verify(post).update("newTitle", "newContent", region);
        assertThat(post.getTitle()).isEqualTo("newTitle");
        assertThat(post.getContent()).isEqualTo("newContent");
        assertThat(post.getRegion().getCode()).isEqualTo(VALID_REGION);
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 수정 시 예외가 반환된다.")
    void givenNoPost_whenUpdate_thenThrowNotFound() {
        // given
        final long POST_ID = 1L;
        final long USER_ID = 10L;
        given(postRepository.findByIdAndDeletedFalse(POST_ID))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
            postService.update(POST_ID, USER_ID, createUpdateDto("t", "c", VALID_REGION))
        )
            .isInstanceOf(PostException.class)
            .extracting("errorCode")
            .isEqualTo(PostErrorCode.NOT_FOUND_POST_ID);
    }


    @Test
    @DisplayName("게시글의 소유자와 수정자가 다른 사용자이면 예외가 발생하는다.")
    void givenPostOfOtherUser_whenUpdate_thenThrowAccessDenied() {
        // given
        final long POST_ID = 1L;
        final long USER_ID = 10L;
        final long OTHER_USER_ID = 20L;
        User user = createUser(USER_ID, "test@test.com", "pw123123", "유저");
        Post post = createPost(POST_ID, user, "title", createRegion(VALID_REGION));
        given(postRepository.findByIdAndDeletedFalse(POST_ID)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() ->
            postService.update(POST_ID, OTHER_USER_ID, createUpdateDto("t", "c", VALID_REGION))
        )
            .isInstanceOf(PostException.class)
            .extracting("errorCode")
            .isEqualTo(PostErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("유효하지 않은 지역 코드로 게시글을 수정 시 예외가 발생하는다.")
    void givenInvalidRegion_whenUpdate_thenThrowRegionNotFound() {
        // given
        final long POST_ID = 1L;
        final long USER_ID = 10L;
        User user = createUser(USER_ID, "test@test.com", "pw123123", "유저");
        Post post = createPost(POST_ID, user, "title", createRegion(INVALID_REGION));
        given(postRepository.findByIdAndDeletedFalse(POST_ID)).willReturn(Optional.of(post));

        given(regionRepository.findById(INVALID_REGION)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
            postService.update(POST_ID, USER_ID, createUpdateDto("t", "c", INVALID_REGION))
        )
            .isInstanceOf(RegionException.class)
            .extracting("errorCode")
            .isEqualTo(RegionErrorCode.NOT_FOUND_REGION_CODE);
    }

    @Test
    @DisplayName("작성자는 게시글을 삭제할 수 있다")
    void givenExistingPostAndOwner_whenDelete_thenSoftDeleted() {
        // given
        long postId = 1L;
        long ownerId = 42L;
        User owner = User.builder().id(ownerId).build();
        Post post = Post.create(owner, "t", "c", createRegion(VALID_REGION));
        assertThat(post.isDeleted()).isFalse();

        given(postRepository.findByIdAndDeletedFalse(postId))
            .willReturn(Optional.of(post));

        // when
        postService.delete(postId, ownerId);

        // then
        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("이미 삭제된 게시글은 다시 삭제할 수 없다")
    void givenDeletedOrNonexistentPost_whenDelete_thenThrowNotFound() {
        // given
        long postId = 99L;
        given(postRepository.findByIdAndDeletedFalse(postId))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.delete(postId, 1L))
            .isInstanceOf(PostException.class)
            .extracting("errorCode")
            .isEqualTo(PostErrorCode.NOT_FOUND_POST_ID);
    }

    @Test
    @DisplayName("작성자만 게시글을 삭제할 수 있다")
    void givenPostOwnedByAnotherUser_whenDelete_thenThrowAccessDenied() {
        // given
        long postId = 2L;
        long ownerId = 42L;
        long otherId = 100L;
        User owner = User.builder().id(ownerId).build();
        Post post = Post.create(owner, "t", "c", createRegion(VALID_REGION));

        given(postRepository.findByIdAndDeletedFalse(postId))
            .willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.delete(postId, otherId))
            .isInstanceOf(PostException.class)
            .extracting("errorCode")
            .isEqualTo(PostErrorCode.ACCESS_DENIED);
    }

    private static User createUser(long userId, String email, String password, String nickname) {
        return User.builder()
            .id(userId)
            .email(email)
            .password(password)
            .nickname(nickname)
            .provider(Provider.LOCAL)
            .role(Role.USER)
            .build();
    }

    private static Region createRegion(String regionCode) {
        return Region.builder()
            .code(regionCode)
            .name("서울특별시 종로구")
            .parentCode("1100000000")
            .level(3)
            .build();
    }

    private static Post createPost(Long postId, User user, String title, Region region) {
        return Post.builder()
            .id(postId)
            .user(user)
            .title(title)
            .region(region)
            .build();
    }

    private UpdatePostRequest createUpdateDto(String title, String content, String regionCode) {
        return new UpdatePostRequest(title, content, regionCode);
    }
}
