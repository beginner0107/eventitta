package com.eventitta.domain.post.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.post.domain.PostLike;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface PostLikeRepository extends BaseRepository<PostLike, Long>, PostLikeRepositoryCustom {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    List<PostLike> findAllByUserId(Long userId);
}
