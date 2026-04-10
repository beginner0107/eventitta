package com.eventitta.domain.post.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.post.domain.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface PostRepository extends BaseRepository<Post, Long>, PostRepositoryCustom {
    Optional<Post> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"images"})
    Optional<Post> findWithImagesByIdAndDeletedFalse(Long id);
}
