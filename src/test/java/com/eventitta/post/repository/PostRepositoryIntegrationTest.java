package com.eventitta.post.repository;

import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.common.config.jpa.QuerydslConfig;
import com.eventitta.post.domain.Post;
import com.eventitta.post.dto.PostFilter;
import com.eventitta.post.dto.SearchType;
import com.eventitta.region.domain.Region;
import com.eventitta.region.repository.RegionRepository;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
@ActiveProfiles("test")
@EntityScan(basePackages = "com.eventitta")
class PostRepositoryIntegrationTest {

    @Autowired
    PostRepository repository;
    @Autowired
    RegionRepository regionRepo;
    @Autowired
    UserRepository userRepo;
    @Autowired
    RefreshTokenRepository rtRepo;

    @BeforeEach
    void setUp() {
        Region r1 = regionRepo.save(new Region("1100110100", "청운효자동", "1100100000", 3));
        User u1 = userRepo.save(
            User.builder()
                .email("a@b.com")
                .password("testPassword")
                .nickname("foo")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .build()
        );
        for (int i = 0; i < 10; i++) {
            String title = (i % 2 == 0 ? "맛집" : "산책") + " 게시글 " + i;
            String content = (i % 3 == 0 ? "테스트" : "내용") + " 본문 " + i;
            repository.save(Post.create(u1, title, content, r1));
        }
    }

    @Test
    @DisplayName("필터 없이 조회하면 모든 게시글이 페이징되어 반환된다")
    void withoutFilter_returnsAllPaged() {
        PostFilter f = new PostFilter(0, 5, null, null, null);
        Page<Post> page = repository.findAllByFilter(f, Pageable.ofSize(5));
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getContent()).hasSize(5);
    }

    @Test
    @DisplayName("제목으로 필터링하면 해당 키워드가 포함된 게시글만 조회된다")
    void filterByTitle_contains맛집() {
        PostFilter f = new PostFilter(0, 10, SearchType.TITLE, "맛집", null);
        List<String> titles = repository
            .findAllByFilter(f, Pageable.ofSize(10))
            .getContent()
            .stream()
            .map(Post::getTitle)
            .toList();
        assertThat(titles).allMatch(t -> t.contains("맛집"));
    }

    @Test
    @DisplayName("제목+내용으로 검색하면 둘 중 하나라도 키워드가 포함된 게시글이 조회된다")
    void filterByTitleOrContent_combined() {
        PostFilter f = new PostFilter(0, 10, SearchType.TITLE_CONTENT, "테스트", null);
        Page<Post> page = repository.findAllByFilter(f, Pageable.ofSize(10));
        assertThat(page.getTotalElements()).isEqualTo(4); // i=0,3,6,9
        assertThat(page.getContent()).allMatch(p ->
            p.getTitle().contains("테스트") || p.getContent().contains("테스트")
        );
    }

    @Test
    @DisplayName("지역 코드로 필터링하면 해당 지역의 게시글만 조회된다")
    void filterByRegionCode() {
        PostFilter f = new PostFilter(0, 10, null, null, "1100110100");
        assertThat(repository.findAllByFilter(f, Pageable.ofSize(10)).getTotalElements())
            .isEqualTo(10);
    }
}
