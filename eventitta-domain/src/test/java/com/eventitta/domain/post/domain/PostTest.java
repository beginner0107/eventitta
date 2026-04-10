package com.eventitta.domain.post.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    private static final Long AUTHOR_USER_ID = 10L;
    private static final String REGION_CODE = "1100110100";

    @DisplayName("게시글 생성 시 작성자 ID와 지역 코드로 초기화된다")
    @Test
    void givenValidFields_whenCreate_thenPostInitializedCorrectly() {
        Post post = Post.create(AUTHOR_USER_ID, "제목", "내용", REGION_CODE);

        assertThat(post.getAuthorUserId()).isEqualTo(AUTHOR_USER_ID);
        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).isEqualTo("내용");
        assertThat(post.getRegionCode()).isEqualTo(REGION_CODE);
        assertThat(post.isDeleted()).isFalse();
    }

    @DisplayName("제목, 내용, 지역 코드가 전달된 값으로 변경된다")
    @Test
    void givenNewFields_whenUpdate_thenFieldsAreUpdated() {
        Post post = Post.create(AUTHOR_USER_ID, "Old Title", "Old Content", REGION_CODE);

        post.update("New Title", "New Content", "2600000000");

        assertThat(post.getTitle()).isEqualTo("New Title");
        assertThat(post.getContent()).isEqualTo("New Content");
        assertThat(post.getRegionCode()).isEqualTo("2600000000");
    }

    @DisplayName("호출 시 삭제 상태로 표시된다")
    @Test
    void whenSoftDelete_thenPostIsMarkedAsDeleted() {
        Post post = Post.create(AUTHOR_USER_ID, "Title", "Content", REGION_CODE);

        post.softDelete();

        assertThat(post.isDeleted()).isTrue();
    }

    @DisplayName("PostImage가 리스트에 추가되고 post 참조가 설정된다")
    @Test
    void givenImage_whenAddImage_thenAddedToListAndLinkedBack() {
        Post post = Post.create(AUTHOR_USER_ID, "제목", "내용", REGION_CODE);
        PostImage image = new PostImage(101L, "url.jpg", 0);

        post.addImage(image);

        assertThat(post.getImages()).contains(image);
        assertThat(image.getPost()).isEqualTo(post);
    }

    @DisplayName("작성자 ID가 일치하면 작성자로 판단한다")
    @Test
    void isAuthor_returnsTrueOnlyForMatchingUserId() {
        Post post = Post.create(AUTHOR_USER_ID, "제목", "내용", REGION_CODE);

        assertThat(post.isAuthor(AUTHOR_USER_ID)).isTrue();
        assertThat(post.isAuthor(999L)).isFalse();
    }
}
