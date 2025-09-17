package com.eventitta.post.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.eventitta.region.domain.Region;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostImageTest {

    @DisplayName("생성자 호출 시 imageUrl과 sortOrder가 설정된다")
    @Test
    void constructor_setsFieldsCorrectly() {
        PostImage image = new PostImage("img.png", 2);

        assertThat(image.getImageUrl()).isEqualTo("img.png");
        assertThat(image.getSortOrder()).isEqualTo(2);
    }

    @DisplayName("setPost() 호출 시 Post와 연관관계가 설정된다")
    @Test
    void setPost_linksToPost() {
        User user = User.builder()
            .email("test@email.com")
            .password("P@ssw0rd123")
            .nickname("tester")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();

        Post post = Post.create(user, "제목", "내용", new Region("123", "서울", null, 1));
        PostImage image = new PostImage("img.png", 1);

        image.setPost(post);

        assertThat(image.getPost()).isEqualTo(post);
    }

    @DisplayName("builder를 사용하면 imageUrl과 sortOrder가 설정된다")
    @Test
    void builder_setsFieldsCorrectly() {
        PostImage image = PostImage.builder()
            .imageUrl("builder.png")
            .sortOrder(5)
            .build();

        assertThat(image.getImageUrl()).isEqualTo("builder.png");
        assertThat(image.getSortOrder()).isEqualTo(5);
    }
}
