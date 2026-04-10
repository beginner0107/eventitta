package com.eventitta.domain.post.service;

import com.eventitta.domain.post.api.internal.facade.PostInternalFacade;
import com.eventitta.domain.post.repository.PostRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.eventitta.domain.post.exception.PostErrorCode.NOT_FOUND_POST_ID;

@Component
@RequiredArgsConstructor
class DefaultPostInternalFacade implements PostInternalFacade {

    private final PostRepository postRepository;

    @Override
    @Transactional(readOnly = true)
    public void ensureActivePost(Long postId) {
        if (postRepository.findByIdAndDeletedFalse(postId).isEmpty()) {
            throw NOT_FOUND_POST_ID.defaultException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findAuthorUserId(Long postId) {
        return postRepository.findByIdAndDeletedFalse(postId)
            .map(post -> post.getAuthorUserId());
    }
}
