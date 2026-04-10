package com.eventitta.domain.post.api.internal.facade;

import java.util.Optional;

public interface PostInternalFacade {

    void ensureActivePost(Long postId);

    Optional<Long> findAuthorUserId(Long postId);

    boolean isMediaAssetReferenced(Long mediaAssetId);
}
