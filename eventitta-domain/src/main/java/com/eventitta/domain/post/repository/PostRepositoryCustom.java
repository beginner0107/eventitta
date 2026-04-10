package com.eventitta.domain.post.repository;

import com.eventitta.domain.post.domain.Post;
import com.eventitta.domain.post.dto.PostFilter;
import com.eventitta.domain.post.dto.response.PostSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
    Page<Post> findAllByFilter(PostFilter filter, Pageable pageable);

    Page<PostSummaryResponse> findSummaries(PostFilter filter, Pageable page, Long currentUserId);
}
