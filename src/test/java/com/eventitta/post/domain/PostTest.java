package com.eventitta.post.domain;

import com.eventitta.region.domain.Region;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    private User user;
    private Region region;
    private Region newRegion;

    @BeforeEach
    void setUp() {
        user = createUser();
        region = new Region("1100100000", "서울", null, 1);
        newRegion = new Region("2600000000", "부산", null, 1);
    }

    private static User createUser() {
        return User.builder()
            .email("test@login.com")
            .password("P@ssw0rd!")
            .nickname("tester")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
    }

    @DisplayName("게시글 생성 시 입력한 값으로 초기화된다")
    @Test
    void givenValidFields_whenCreate_thenPostInitializedCorrectly() {
        Post post = Post.create(user, "제목", "내용", region);

        assertThat(post.getUser()).isEqualTo(user);
        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).isEqualTo("내용");
        assertThat(post.getRegion()).isEqualTo(region);
        assertThat(post.isDeleted()).isFalse();
    }

    @DisplayName("제목, 내용, 지역이 전달된 값으로 변경된다")
    @Test
    void givenNewFields_whenUpdate_thenFieldsAreUpdated() {
        Post post = Post.create(user, "Old Title", "Old Content", region);
        post.update("New Title", "New Content", newRegion);

        assertThat(post.getTitle()).isEqualTo("New Title");
        assertThat(post.getContent()).isEqualTo("New Content");
        assertThat(post.getRegion()).isEqualTo(newRegion);
    }

    @DisplayName("호출 시 삭제 상태로 표시된다")
    @Test
    void whenSoftDelete_thenPostIsMarkedAsDeleted() {
        Post post = Post.create(user, "Title", "Content", region);
        post.softDelete();

        assertThat(post.isDeleted()).isTrue();
    }

    @DisplayName("PostImage가 리스트에 추가되고 post 참조가 설정된다")
    @Test
    void givenImage_whenAddImage_thenAddedToListAndLinkedBack() {
        Post post = Post.create(user, "제목", "내용", region);
        PostImage image = new PostImage("url.jpg", 0);
        post.addImage(image);

        assertThat(post.getImages()).contains(image);
        assertThat(image.getPost()).isEqualTo(post);
    }

    @DisplayName("PostImage가 리스트에서 제거되고 post 참조가 해제된다")
    @Test
    void givenImage_whenRemoveImage_thenRemovedFromListAndUnlinked() {
        Post post = Post.create(user, "제목", "내용", region);
        PostImage image = new PostImage("url.jpg", 0);
        post.addImage(image);
        post.removeImage(image);

        assertThat(post.getImages()).doesNotContain(image);
        assertThat(image.getPost()).isNull();
    }

    @DisplayName("모든 PostImage가 제거되고 post 참조가 해제된다")
    @Test
    void whenClearImages_thenAllImagesRemoved() {
        Post post = Post.create(user, "제목", "내용", region);
        PostImage img1 = new PostImage("img1.jpg", 0);
        PostImage img2 = new PostImage("img2.jpg", 1);
        post.addImage(img1);
        post.addImage(img2);

        post.clearImages();

        assertThat(post.getImages()).isEmpty();
        assertThat(img1.getPost()).isNull();
        assertThat(img2.getPost()).isNull();
    }
}
