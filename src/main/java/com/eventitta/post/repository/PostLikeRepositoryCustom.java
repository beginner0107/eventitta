package com.eventitta.post.repository;

import com.eventitta.post.dto.response.PostSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostLikeRepositoryCustom {
    Page<PostSummaryResponse> findLikedSummaries(Long userId, Pageable pageable);
}

