package com.eventitta.domain.media.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.media.domain.MediaAssetVariant;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.List;

@NoRepositoryBean
public interface MediaAssetVariantRepository extends BaseRepository<MediaAssetVariant, Long> {

    List<MediaAssetVariant> findAllByMediaAssetIdIn(Collection<Long> mediaAssetIds);

    List<MediaAssetVariant> findAllByMediaAssetId(Long mediaAssetId);

    void deleteAllByMediaAssetId(Long mediaAssetId);
}
