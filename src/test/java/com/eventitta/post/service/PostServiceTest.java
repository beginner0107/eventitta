package com.eventitta.post.service;

import com.eventitta.post.domain.Post;
import com.eventitta.post.dto.request.CreatePostRequest;
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

    private static Post createPost(Long userId, User user, String title, Region region) {
        return Post.builder()
            .id(userId)
            .user(user)
            .title(title)
            .region(region)
            .build();
    }
}
