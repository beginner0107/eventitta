package com.eventitta.domain.comment.api.internal.facade;

public interface CommentQueryFacade {

    int countActiveCommentsByPostId(Long postId);
}
