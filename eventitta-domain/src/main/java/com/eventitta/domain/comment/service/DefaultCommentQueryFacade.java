package com.eventitta.domain.comment.service;

import com.eventitta.domain.comment.api.internal.facade.CommentQueryFacade;
import com.eventitta.domain.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultCommentQueryFacade implements CommentQueryFacade {

    private final CommentRepository commentRepository;

    @Override
    public int countActiveCommentsByPostId(Long postId) {
        return commentRepository.countByPostIdAndDeletedFalse(postId);
    }
}
