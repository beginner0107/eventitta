package com.eventitta.post.repository;

import com.eventitta.post.domain.Post;
import com.eventitta.post.dto.PostFilter;
import com.eventitta.post.dto.response.PostSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
    Page<Post> findAllByFilter(PostFilter filter, Pageable pageable);

    Page<PostSummaryDto> findSummaries(PostFilter filter, Pageable page);
}
