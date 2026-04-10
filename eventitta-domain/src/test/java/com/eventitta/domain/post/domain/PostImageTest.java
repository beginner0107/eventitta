package com.eventitta.domain.post.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostImageTest {

    @DisplayName("생성자 호출 시 mediaAssetId, imageUrl과 sortOrder가 설정된다")
    @Test
    void constructor_setsFieldsCorrectly() {
        PostImage image = new PostImage(55L, "img.png", 2);

        assertThat(image.getMediaAssetId()).isEqualTo(55L);
        assertThat(image.getImageUrl()).isEqualTo("img.png");
        assertThat(image.getSortOrder()).isEqualTo(2);
    }

    @DisplayName("setPost() 호출 시 Post와 연관관계가 설정된다")
    @Test
    void setPost_linksToPost() {
        Post post = Post.create(1L, "제목", "내용", "1100110100");
        PostImage image = new PostImage(99L, "img.png", 1);

        image.setPost(post);

        assertThat(image.getPost()).isEqualTo(post);
    }
}
