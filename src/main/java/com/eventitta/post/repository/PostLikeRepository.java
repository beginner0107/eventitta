package com.eventitta.post.repository;

import com.eventitta.post.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long>, PostLikeRepositoryCustom {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    List<PostLike> findAllByUserId(Long userId);
}
