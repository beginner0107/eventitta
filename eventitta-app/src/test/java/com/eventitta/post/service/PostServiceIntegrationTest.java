package com.eventitta.domain.post.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.domain.gamification.api.internal.facade.GamificationInternalFacade;
import com.eventitta.domain.post.domain.Post;
import com.eventitta.domain.post.domain.PostLike;
import com.eventitta.domain.post.dto.PostFilter;
import com.eventitta.domain.post.dto.response.PostDetailResponse;
import com.eventitta.domain.post.dto.response.PostLikeStateResponse;
import com.eventitta.domain.post.dto.response.PostSummaryResponse;
import com.eventitta.domain.post.exception.PostException;
import com.eventitta.domain.post.repository.PostLikeRepository;
import com.eventitta.domain.post.repository.PostRepository;
import com.eventitta.domain.region.domain.Region;
import com.eventitta.domain.region.repository.RegionRepository;
import com.eventitta.domain.user.domain.Provider;
import com.eventitta.domain.user.domain.Role;
import com.eventitta.domain.user.domain.User;
import com.eventitta.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.eventitta.domain.post.exception.PostErrorCode.NOT_FOUND_POST_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@Transactional
@DisplayName("게시글 서비스 통합 테스트")
class PostServiceIntegrationTest extends IntegrationTestSupport {

    private static final String REGION_CODE = "1100110100";

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private GamificationInternalFacade gamificationFacade;

    @Test
    @DisplayName("좋아요 요청을 반복해도 한 번만 저장되고 좋아요 게임화는 발생하지 않는다")
    void repeatedLikeRequest_keepsLikedStateWithoutGamification() {
        // given
        User actor = saveUser("actor-like@test.com", "actor-like");
        Post post = savePost(actor.getId(), "좋아요 테스트 글");

        // when
        PostLikeStateResponse first = postService.likePost(post.getId(), actor.getId());
        PostLikeStateResponse second = postService.likePost(post.getId(), actor.getId());

        // then
        assertThat(first.likedByMe()).isTrue();
        assertThat(first.likeCount()).isEqualTo(1);
        assertThat(second.likedByMe()).isTrue();
        assertThat(second.likeCount()).isEqualTo(1);
        assertThat(postLikeRepository.countByPostId(post.getId())).isEqualTo(1L);
        verifyNoInteractions(gamificationFacade);
    }

    @Test
    @DisplayName("좋아요 취소 요청을 반복해도 한 번만 삭제되고 좋아요 게임화는 발생하지 않는다")
    void repeatedUnlikeRequest_keepsUnlikedStateWithoutGamification() {
        // given
        User actor = saveUser("actor-unlike@test.com", "actor-unlike");
        Post post = savePost(actor.getId(), "취소 테스트 글");
        postLikeRepository.saveAndFlush(new PostLike(post, actor.getId()));

        // when
        PostLikeStateResponse first = postService.unlikePost(post.getId(), actor.getId());
        PostLikeStateResponse second = postService.unlikePost(post.getId(), actor.getId());

        // then
        assertThat(first.likedByMe()).isFalse();
        assertThat(first.likeCount()).isZero();
        assertThat(second.likedByMe()).isFalse();
        assertThat(second.likeCount()).isZero();
        assertThat(postLikeRepository.countByPostId(post.getId())).isZero();
        verifyNoInteractions(gamificationFacade);
    }

    @Test
    @DisplayName("목록과 상세는 posts.like_count 가 틀려도 post_likes 기준으로 좋아요 수를 계산한다")
    void getPostsAndDetail_usePostLikesAsSourceOfTruth() {
        // given
        User author = saveUser("author-count@test.com", "author-count");
        User liker = saveUser("liker-count@test.com", "liker-count");
        User otherLiker = saveUser("other-liker-count@test.com", "other-liker-count");
        Post post = savePost(author.getId(), "정합성 테스트 글");
        postLikeRepository.saveAllAndFlush(List.of(
            new PostLike(post, liker.getId()),
            new PostLike(post, otherLiker.getId())
        ));
        updateStoredLikeCount(post.getId(), 99);
        entityManager.clear();

        // when
        PostDetailResponse detail = postService.getPost(post.getId(), liker.getId());
        List<PostSummaryResponse> summaries = postService.getPosts(liker.getId(), new PostFilter(0, 10, null, null, null))
            .content();

        // then
        assertThat(detail.likeCount()).isEqualTo(2);
        assertThat(detail.likedByMe()).isTrue();
        assertThat(summaries).singleElement().satisfies(summary -> {
            assertThat(summary.likeCount()).isEqualTo(2);
            assertThat(summary.likedByMe()).isTrue();
        });
    }

    @Test
    @DisplayName("좋아요한 게시글 목록은 likedByMe=true 와 post_likes 기준 likeCount 를 반환한다")
    void getLikedPosts_returnsDerivedLikeStateAndCount() {
        // given
        User author = saveUser("author-liked@test.com", "author-liked");
        User liker = saveUser("liker-liked@test.com", "liker-liked");
        User otherLiker = saveUser("other-liked@test.com", "other-liked");
        Post post = savePost(author.getId(), "좋아요 목록 테스트 글");
        postLikeRepository.saveAllAndFlush(List.of(
            new PostLike(post, liker.getId()),
            new PostLike(post, otherLiker.getId())
        ));
        updateStoredLikeCount(post.getId(), 77);
        entityManager.clear();

        // when
        List<PostSummaryResponse> likedPosts = postService.getLikedPosts(liker.getId(), new PostFilter(0, 10, null, null, null))
            .content();

        // then
        assertThat(likedPosts).singleElement().satisfies(summary -> {
            assertThat(summary.likeCount()).isEqualTo(2);
            assertThat(summary.likedByMe()).isTrue();
        });
    }

    @Test
    @DisplayName("삭제된 게시글은 좋아요와 좋아요 취소 대상이 될 수 없다")
    void likeActions_onDeletedPost_throwNotFound() {
        // given
        User actor = saveUser("actor-deleted@test.com", "actor-deleted");
        Post post = savePost(actor.getId(), "삭제된 게시글");
        post.softDelete();
        postRepository.saveAndFlush(post);
        entityManager.clear();

        // when // then
        assertThatThrownBy(() -> postService.likePost(post.getId(), actor.getId()))
            .isInstanceOf(PostException.class)
            .hasMessageContaining(NOT_FOUND_POST_ID.defaultMessage());

        assertThatThrownBy(() -> postService.unlikePost(post.getId(), actor.getId()))
            .isInstanceOf(PostException.class)
            .hasMessageContaining(NOT_FOUND_POST_ID.defaultMessage());
    }

    private User saveUser(String email, String nickname) {
        return userRepository.saveAndFlush(User.builder()
            .email(email)
            .password("encoded-password")
            .nickname(nickname)
            .provider(Provider.LOCAL)
            .role(Role.USER)
            .deleted(false)
            .build());
    }

    private Post savePost(Long authorUserId, String title) {
        saveRegionIfMissing();
        return postRepository.saveAndFlush(Post.create(authorUserId, title, "본문", REGION_CODE));
    }

    private void saveRegionIfMissing() {
        if (regionRepository.findById(REGION_CODE).isEmpty()) {
            regionRepository.saveAndFlush(new Region(REGION_CODE, "청운효자동", "1100100000", 3));
        }
    }

    private void updateStoredLikeCount(Long postId, int likeCount) {
        entityManager.createNativeQuery("UPDATE posts SET like_count = :likeCount WHERE id = :postId")
            .setParameter("likeCount", likeCount)
            .setParameter("postId", postId)
            .executeUpdate();
        entityManager.flush();
    }
}
