package com.eventitta.domain.post.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.post.domain.PostImage;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface PostImageRepository extends BaseRepository<PostImage, Long> {
    boolean existsByMediaAssetId(Long mediaAssetId);
}
