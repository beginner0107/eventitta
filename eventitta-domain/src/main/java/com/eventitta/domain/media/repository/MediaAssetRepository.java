package com.eventitta.domain.media.repository;

import com.eventitta.domain.common.repository.BaseRepository;
import com.eventitta.domain.media.domain.MediaAsset;
import com.eventitta.domain.media.domain.MediaAssetStatus;
import com.eventitta.domain.media.domain.MediaProcessingStatus;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@NoRepositoryBean
public interface MediaAssetRepository extends BaseRepository<MediaAsset, Long> {

    List<MediaAsset> findAllByIdIn(Collection<Long> ids);

    List<MediaAsset> findAllByStatusAndCreatedAtBefore(MediaAssetStatus status, LocalDateTime cutoff);

    List<MediaAsset> findAllByStatusAndUpdatedAtBefore(MediaAssetStatus status, LocalDateTime cutoff);

    List<MediaAsset> findAllByStorageKeyAndIdNotAndStatusNot(String storageKey, Long id, MediaAssetStatus status);

    List<MediaAsset> findAllByProcessingStatus(MediaProcessingStatus processingStatus);
}
