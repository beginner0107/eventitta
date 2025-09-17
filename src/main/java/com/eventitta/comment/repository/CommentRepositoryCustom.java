package com.eventitta.comment.repository;

import com.eventitta.comment.dto.query.CommentFlatDto;

import java.util.List;

public interface CommentRepositoryCustom {
    List<CommentFlatDto> findFlatByPost(Long postId);
}
