package com.eventitta.comment.repository;

import com.eventitta.comment.dto.projection.CommentFlatProjection;

import java.util.List;

public interface CommentRepositoryCustom {
    List<CommentFlatProjection> findFlatByPost(Long postId);
}
