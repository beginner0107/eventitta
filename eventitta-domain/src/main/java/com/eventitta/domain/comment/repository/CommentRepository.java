package com.eventitta.domain.comment.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.comment.domain.Comment;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface CommentRepository extends BaseRepository<Comment, Long>, CommentRepositoryCustom {

    List<Comment> findAllByPostIdAndParentIsNullOrderByCreatedAtAsc(Long postId);

    int countByPostIdAndDeletedFalse(Long postId);
}
