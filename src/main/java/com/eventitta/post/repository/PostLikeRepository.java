package com.eventitta.post.repository;

import com.eventitta.post.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long>, PostLikeRepositoryCustom {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    List<PostLike> findAllByUserId(Long userId);
}
