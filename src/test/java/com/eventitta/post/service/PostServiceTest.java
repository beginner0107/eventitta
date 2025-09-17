package com.eventitta.post.service;

import com.eventitta.auth.exception.AuthException;
import com.eventitta.comment.repository.CommentRepository;
import com.eventitta.common.response.PageResponse;
import com.eventitta.gamification.activitylog.ActivityEventPublisher;
import com.eventitta.post.domain.Post;
import com.eventitta.post.domain.PostImage;
import com.eventitta.post.domain.PostLike;
import com.eventitta.post.dto.PostFilter;
import com.eventitta.post.dto.request.CreatePostRequest;
import com.eventitta.post.dto.request.UpdatePostRequest;
import com.eventitta.post.dto.response.CreatePostResponse;
import com.eventitta.post.dto.response.PostDetailDto;
import com.eventitta.post.dto.response.PostSummaryDto;
import com.eventitta.post.event.PostDeleteEventPublisher;
import com.eventitta.post.event.PostDeletedEvent;
import com.eventitta.post.exception.PostErrorCode;
import com.eventitta.post.exception.PostException;
import com.eventitta.post.repository.PostLikeRepository;
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
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.eventitta.gamification.constant.ActivityCodes.CREATE_POST;
import static com.eventitta.gamification.constant.ActivityCodes.LIKE_POST;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("동네 기반 게시글 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    PostRepository postRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    RegionRepository regionRepository;
    @Mock
    PostLikeRepository postLikeRepository;
    @Mock
    CommentRepository commentRepository;
    @Mock
    ActivityEventPublisher activityEventPublisher;
    @Mock
    PostDeleteEventPublisher postDeleteEventPublisher;

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
        CreatePostRequest createPostRequest = new CreatePostRequest("제목", "내용", regionCode, List.of("url1", "url2"));

        User user = createUser(userId, "test@test.com", "pw123123", "유저");
        Region region = createRegion(regionCode);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(regionRepository.findById(regionCode)).willReturn(Optional.of(region));

        Post savedPost = createPost(123L, user, createPostRequest.title(), createPostRequest.content(), region);
        savedPost.addImage(new PostImage("url1", 0));
        savedPost.addImage(new PostImage("url2", 1));
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        CreatePostResponse result = postService.create(userId, createPostRequest);

        // then
        assertThat(result.id()).isEqualTo(123L);
        assertThat(savedPost.getImages()).hasSize(2);

        // 이벤트 발행 검증
        verify(activityEventPublisher).publish(CREATE_POST, userId, savedPost.getId());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 생성 시 예외가 반환된다.")
    void whenUserNotFound_thenThrowUserNotFound() {
        // given
        long fakeUserId = 99L;
        CreatePostRequest dto = new CreatePostRequest("제목", "내용", "1100110100", List.of());
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
        CreatePostRequest dto = new CreatePostRequest("제목", "내용", "0000000000", List.of());
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
        given(postRepository.findWithUserByIdAndDeletedFalse(POST_ID))
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
        given(postRepository.findWithUserByIdAndDeletedFalse(POST_ID))
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
        Post post = createPost(POST_ID, user, "title", "content", createRegion(VALID_REGION));
        given(postRepository.findWithUserByIdAndDeletedFalse(POST_ID)).willReturn(Optional.of(post));

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
        Post post = createPost(POST_ID, user, "title", "content", createRegion(INVALID_REGION));
        given(postRepository.findWithUserByIdAndDeletedFalse(POST_ID)).willReturn(Optional.of(post));

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
    @DisplayName("업데이트 시 기존 이미지가 제거되고 새로운 이미지로 대체된다")
    void givenImages_whenUpdate_thenImagesReplaced() {
        // given
        final long POST_ID = 1L;
        final long USER_ID = 10L;
        Region region = createRegion(VALID_REGION);
        Post post = spy(Post.create(createUser(USER_ID, "a@b.com", "pw", "nick"), "title", "content", region));
        given(postRepository.findWithUserByIdAndDeletedFalse(POST_ID)).willReturn(Optional.of(post));
        given(regionRepository.findById(VALID_REGION)).willReturn(Optional.of(region));

        List<String> imageUrls = List.of("img1.jpg", "img2.jpg", "img3.jpg");
        UpdatePostRequest dto = new UpdatePostRequest("updated", "updated content", VALID_REGION, imageUrls);

        // when
        postService.update(POST_ID, USER_ID, dto);

        // then
        verify(post).clearImages();
        assertThat(post.getImages()).hasSize(3);
        assertThat(post.getImages())
            .extracting(PostImage::getImageUrl)
            .containsExactly("img1.jpg", "img2.jpg", "img3.jpg");
    }

    @Test
    @DisplayName("작성자는 게시글을 삭제할 수 있다")
    void givenExistingPostAndOwner_whenDelete_thenSoftDeleted() {
        // given
        long postId = 1L;
        long ownerId = 42L;
        User owner = User.builder().id(ownerId).build();
        Post post = spy(Post.create(owner, "t", "c", createRegion(VALID_REGION)));
        post.addImage(new PostImage("img1.jpg", 0));

        given(postRepository.findWithUserByIdAndDeletedFalse(postId))
            .willReturn(Optional.of(post));

        // when
        postService.delete(postId, ownerId);

        // then
        assertThat(post.isDeleted()).isTrue();
        verify(postDeleteEventPublisher).publish(any(PostDeletedEvent.class));
    }

    @Test
    @DisplayName("이미 삭제된 게시글은 다시 삭제할 수 없다")
    void givenDeletedOrNonexistentPost_whenDelete_thenThrowNotFound() {
        // given
        long postId = 99L;
        given(postRepository.findWithUserByIdAndDeletedFalse(postId))
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

        given(postRepository.findWithUserByIdAndDeletedFalse(postId))
            .willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.delete(postId, otherId))
            .isInstanceOf(PostException.class)
            .extracting("errorCode")
            .isEqualTo(PostErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("결과가 없는 경우 빈 리스트와 함께 페이지 정보가 제공된다")
    void givenEmptyPage_whenGetPosts_thenReturnEmptyPageResponse() {
        // given
        PostFilter filter = new PostFilter(0, 5, null, null, null);
        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Page<PostSummaryDto> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(postRepository.findSummaries(eq(filter), any(Pageable.class))).willReturn(emptyPage);

        // when
        PageResponse<PostSummaryDto> response = postService.getPosts(filter);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    @DisplayName("여러 게시글을 조회하면 응답에 올바른 페이징 정보가 포함된다")
    void givenNonEmptyPage_whenGetPosts_thenMapToPageResponse() {
        // given
        PostFilter filter = new PostFilter(1, 2, null, null, null);
        Pageable pageable = PageRequest.of(1, 2, Sort.by("createdAt").descending());

        User user1 = User.builder().id(10L).build();
        User user2 = User.builder().id(11L).build();
        Post p1 = createPost(10L, user1, "t1", "c1", createRegion(VALID_REGION));
        Post p2 = createPost(11L, user2, "t2", "c2", createRegion(VALID_REGION));
        PostSummaryDto p1Dto = PostSummaryDto.from(p1);
        PostSummaryDto p2Dto = PostSummaryDto.from(p2);
        List<PostSummaryDto> content = List.of(p1Dto, p2Dto);
        long total = 7;
        Page<PostSummaryDto> page = new PageImpl<>(content, pageable, total);

        given(postRepository.findSummaries(eq(filter), any(Pageable.class)))
            .willReturn(page);
        // when
        PageResponse<PostSummaryDto> response = postService.getPosts(filter);

        // then
        assertThat(response.content())
            .extracting(PostSummaryDto::id, PostSummaryDto::title)
            .containsExactly(
                tuple(10L, "t1"),
                tuple(11L, "t2")
            );

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(7);
        assertThat(response.totalPages()).isEqualTo(4);
    }

    @Test
    @DisplayName("존재하는 게시글을 조회하면 게시글이 조회된다")
    void givenPostId_whenValidPostId_thenReturnPostResponse() {
        // given
        Long postId = 123L;
        User author = createUser(10L, "a@b.com", "pw12345678", "nick");
        Region region = createRegion("1100110100");
        Post post = Post.create(author, "제목", "내용", region);

        given(postRepository.findDetailByIdAndDeletedFalse(postId))
            .willReturn(Optional.of(post));
        given(commentRepository.countByPostIdAndDeletedFalse(postId))
            .willReturn(1);

        // when
        PostDetailDto response = postService.getPost(postId);

        // then
        assertThat(response.id()).isEqualTo(post.getId());
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("내용");
        assertThat(response.regionCode()).isEqualTo("1100110100");
        assertThat(response.commentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 조회하면 게시글을 조회할 수 없다.")
    void whenPostNotFound_thenThrowPostException() {
        // given
        Long postId = 999L;
        given(postRepository.findDetailByIdAndDeletedFalse(postId))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPost(postId))
            .isInstanceOf(PostException.class)
            .extracting("errorCode")
            .isEqualTo(PostErrorCode.NOT_FOUND_POST_ID);
    }

    @Test
    @DisplayName("게시글 생성 시 이미지의 sortOrder가 입력 순서대로 설정된다")
    void givenImageUrls_whenCreate_thenSortOrderPreserved() {
        // given
        long userId = 1L;
        String regionCode = VALID_REGION;
        CreatePostRequest dto = new CreatePostRequest("제목", "내용", regionCode, List.of("img1", "img2", "img3"));
        User user = createUser(userId, "test@user.com", "pw", "nick");
        Region region = createRegion(regionCode);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(regionRepository.findById(regionCode)).willReturn(Optional.of(region));

        final Post[] createdPostHolder = new Post[1];
        given(postRepository.save(any())).willAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            createdPostHolder[0] = post;
            return post;
        });

        // when
        CreatePostResponse result = postService.create(userId, dto);

        // then
        Post createdPost = createdPostHolder[0];
        assertThat(createdPost.getImages())
            .extracting(PostImage::getSortOrder)
            .containsExactly(0, 1, 2);

        // 이벤트 발행 검증
        verify(activityEventPublisher).publish(CREATE_POST, userId, createdPost.getId());
    }

    @Test
    @DisplayName("이미지가 없는 경우에도 게시글 생성이 정상 동작한다")
    void givenNullImageUrls_whenCreate_thenNoExceptionThrown() {
        // given
        long userId = 1L;
        String regionCode = VALID_REGION;
        CreatePostRequest dto = new CreatePostRequest("제목", "내용", regionCode, null);
        User user = createUser(userId, "test@user.com", "pw", "nick");
        Region region = createRegion(regionCode);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(regionRepository.findById(regionCode)).willReturn(Optional.of(region));
        given(postRepository.save(any())).willAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "id", 123L);
            return post;
        });

        // when
        CreatePostResponse response = postService.create(userId, dto);

        // then
        assertThat(response.id()).isNotNull();

        // 이벤트 발행 검증
        verify(activityEventPublisher).publish(CREATE_POST, userId, 123L);
    }

    @Test
    @DisplayName("게시글을 추천하면 추천 개수가 증가한다.")
    void toggleLike_firstTime_addLike() {
        // given
        Long postId = 1L;
        Long userId = 10L;

        Post post = mock(Post.class);
        User user = mock(User.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(postLikeRepository.findByPostIdAndUserId(postId, userId)).willReturn(Optional.empty());

        // when
        postService.toggleLike(postId, userId);

        // then
        verify(postLikeRepository).save(any(PostLike.class));
        verify(post).incrementLikeCount();

        // 이벤트 발행 검증
        verify(activityEventPublisher).publish(LIKE_POST, userId, postId);
    }

    @Test
    @DisplayName("게시글을 이미 추천한 경우 추천을 취소한다.")
    void toggleLike_alreadyLiked_removeLike() {
        // given
        Long postId = 1L;
        Long userId = 10L;

        Post post = mock(Post.class);
        User user = mock(User.class);
        PostLike like = mock(PostLike.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(postLikeRepository.findByPostIdAndUserId(postId, userId)).willReturn(Optional.of(like));

        // when
        postService.toggleLike(postId, userId);

        // then
        verify(postLikeRepository).delete(like);
        verify(post).decrementLikeCount();

        // 이벤트 발행 검증
        verify(activityEventPublisher).publishRevoke(LIKE_POST, userId, postId);
    }

    @Test
    @DisplayName("유저가 존재하지 않으면 예외가 발생한다")
    void toggleLike_userNotFound() {
        Long postId = 1L;
        Long userId = 10L;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThrows(AuthException.class, () -> postService.toggleLike(postId, userId));
    }

    @Test
    @DisplayName("게시글이 존재하지 않으면 예외가 발생한다")
    void toggleLike_postNotFound() {
        Long postId = 1L;
        Long userId = 10L;
        User user = mock(User.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        assertThrows(PostException.class, () -> postService.toggleLike(postId, userId));
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

    private static Post createPost(Long postId, User user, String title, String content, Region region) {
        return Post.builder()
            .id(postId)
            .user(user)
            .title(title)
            .content(content)
            .region(region)
            .build();
    }

    private UpdatePostRequest createUpdateDto(String title, String content, String regionCode) {
        return new UpdatePostRequest(title, content, regionCode, List.of());
    }
}
