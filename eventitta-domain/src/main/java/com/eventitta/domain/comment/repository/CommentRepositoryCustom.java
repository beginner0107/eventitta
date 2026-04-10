package com.eventitta.domain.comment.repository;

import com.eventitta.domain.comment.dto.projection.CommentFlatProjection;

import java.util.List;

public interface CommentRepositoryCustom {
    List<CommentFlatProjection> findFlatByPost(Long postId);
}
