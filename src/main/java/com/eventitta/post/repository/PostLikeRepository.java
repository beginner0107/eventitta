package com.eventitta.post.repository;

import com.eventitta.post.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    List<PostLike> findAllByUserId(Long userId);

    long countByPostId(Long postId);
}
