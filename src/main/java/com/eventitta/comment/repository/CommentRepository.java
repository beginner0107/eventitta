package com.eventitta.comment.repository;

import com.eventitta.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    List<Comment> findAllByPostIdAndParentIsNullOrderByCreatedAtAsc(Long postId);

    int countByPostIdAndDeletedFalse(Long postId);
}
