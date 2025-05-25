package com.eventitta.post.repository;

import com.eventitta.post.domain.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {
    @EntityGraph(attributePaths = {"user"})
    Optional<Post> findWithUserByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"user", "region"})
    Optional<Post> findDetailByIdAndDeletedFalse(Long id);
}
