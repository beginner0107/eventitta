package com.eventitta.domain.media.service;

import com.eventitta.domain.file.api.internal.FileStorageProvider;
import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.api.internal.facade.FileStorageFacade;
import com.eventitta.domain.media.api.internal.facade.MediaAssetInternalFacade;
import com.eventitta.domain.media.api.internal.view.MediaAttachmentView;
import com.eventitta.domain.media.api.internal.view.MediaDisplayView;
import com.eventitta.domain.media.api.internal.view.UploadedMediaView;
import com.eventitta.domain.media.domain.MediaAsset;
import com.eventitta.domain.media.domain.MediaAssetStatus;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.media.domain.MediaAssetVariant;
import com.eventitta.domain.media.exception.MediaAssetErrorCode;
import com.eventitta.domain.media.policy.MediaCategoryPolicy;
import com.eventitta.domain.media.policy.MediaCleanupPolicy;
import com.eventitta.domain.media.policy.MediaPolicyProvider;
import com.eventitta.domain.media.repository.MediaAssetRepository;
import com.eventitta.domain.media.repository.MediaAssetVariantRepository;
import com.eventitta.domain.post.api.internal.facade.PostInternalFacade;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaAssetService implements MediaAssetInternalFacade {

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAssetVariantRepository mediaAssetVariantRepository;
    private final UserInternalFacade userInternalFacade;
    private final PostInternalFacade postInternalFacade;
    private final FileStorageFacade fileStorageService;
    private final MediaUploadValidationService mediaUploadValidationService;
    private final MediaPolicyProvider mediaPolicyProvider;
    private final MediaUrlResolver mediaUrlResolver;
    private final MediaVariantProcessingService mediaVariantProcessingService;
    private final Clock clock;

    @Transactional
    @Override
    public List<UploadedMediaView> upload(Long userId, MediaCategory category, List<UploadFileCommand> files) {
        userInternalFacade.ensureActiveUser(userId);
        List<ValidatedMedia> validatedFiles = mediaUploadValidationService.validateFiles(category, files);
        String keyPrefix = "media/original/" + category.storageDirectory() + "/" + userId;

        List<String> storedKeys = new ArrayList<>();
        List<MediaAsset> assets = new ArrayList<>();

        try {
            for (int i = 0; i < files.size(); i++) {
                UploadFileCommand file = files.get(i);
                ValidatedMedia validatedFile = validatedFiles.get(i);
                String storageKey = fileStorageService.store(file, keyPrefix);
                storedKeys.add(storageKey);

                MediaAsset asset = MediaAsset.temp(
                    userId,
                    category,
                    fileStorageService.getStorageProvider(),
                    storageKey,
                    mediaUrlResolver.resolveOriginalUrl(storageKey),
                    validatedFile.originalFilename(),
                    validatedFile.contentType(),
                    validatedFile.sizeBytes(),
                    validatedFile.checksum(),
                    validatedFile.width(),
                    validatedFile.height()
                );
                assets.add(asset);
            }

            List<MediaAsset> savedAssets = mediaAssetRepository.saveAllAndFlush(assets);
            triggerAsyncVariantProcessing(savedAssets);
            log.info("[MediaAsset] upload complete userId={}, category={}, count={}", userId, category, savedAssets.size());
            return savedAssets.stream()
                .map(asset -> new UploadedMediaView(
                    asset.getId(),
                    mediaUrlResolver.resolveUploadUrl(asset),
                    asset.getContentType(),
                    asset.getSizeBytes(),
                    asset.getStatus(),
                    asset.getProcessingStatus()
                ))
                .toList();
        } catch (RuntimeException ex) {
            cleanupStoredKeys(storedKeys);
            throw ex;
        }
    }

    @Transactional
    @Override
    public List<MediaAttachmentView> resolveAndAttachAssets(Long userId, MediaCategory category, List<Long> mediaIds) {
        List<MediaAsset> assets = resolveAttachableAssets(userId, category, mediaIds);
        attachAssets(assets);
        return assets.stream()
            .map(asset -> new MediaAttachmentView(asset.getId(), mediaUrlResolver.resolveUploadUrl(asset)))
            .toList();
    }

    @Override
    public String resolveDisplayUrl(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaAsset asset = mediaAssetRepository.findById(mediaId)
            .orElse(null);
        return asset != null ? mediaUrlResolver.resolveDisplayInfo(asset).imageUrl() : null;
    }

    @Override
    public Map<Long, MediaDisplayView> resolveDisplayViews(Collection<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Map.of();
        }

        List<MediaAsset> assets = mediaAssetRepository.findAllByIdIn(new LinkedHashSet<>(mediaIds));
        Map<Long, MediaDisplayInfo> displayInfos = mediaUrlResolver.resolveDisplayInfos(assets);
        Map<Long, MediaDisplayView> results = new LinkedHashMap<>();
        for (Long mediaId : mediaIds) {
            MediaDisplayInfo info = displayInfos.get(mediaId);
            if (info == null) {
                continue;
            }
            results.put(
                mediaId,
                new MediaDisplayView(
                    info.imageUrl(),
                    info.thumbnailUrl(),
                    info.width(),
                    info.height(),
                    info.processingStatus()
                )
            );
        }
        return results;
    }

    public List<MediaAsset> resolveAttachableAssets(Long userId, MediaCategory category, List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return List.of();
        }

        validateRequestedMediaIds(category, mediaIds);

        Map<Long, MediaAsset> assetMap = mediaAssetRepository.findAllByIdIn(mediaIds).stream()
            .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));

        List<MediaAsset> orderedAssets = new ArrayList<>();
        for (Long mediaId : mediaIds) {
            MediaAsset asset = assetMap.get(mediaId);
            if (asset == null) {
                throw MediaAssetErrorCode.NOT_FOUND_MEDIA_ASSET.defaultException();
            }
            if (!asset.isOwnedBy(userId)) {
                throw MediaAssetErrorCode.MEDIA_ASSET_ACCESS_DENIED.defaultException();
            }
            if (asset.getCategory() != category) {
                throw MediaAssetErrorCode.MEDIA_ASSET_CATEGORY_MISMATCH.defaultException();
            }
            if (!asset.isAttachable()) {
                throw MediaAssetErrorCode.MEDIA_ASSET_NOT_ATTACHABLE.defaultException();
            }
            orderedAssets.add(asset);
        }
        return orderedAssets;
    }

    @Transactional
    public void attachAssets(Collection<MediaAsset> assets) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (MediaAsset asset : assets) {
            asset.attach(mediaUrlResolver.resolveUploadUrl(asset), now);
        }
    }

    @Transactional
    @Override
    public void releaseAssetsIfUnreferenced(Collection<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return;
        }

        List<MediaAsset> assets = mediaAssetRepository.findAllByIdIn(new LinkedHashSet<>(mediaIds));
        for (MediaAsset asset : assets) {
            if (!isReferenced(asset.getId())) {
                asset.release();
            }
        }
    }

    @Transactional
    public void cleanupStaleAssets() {
        LocalDateTime now = LocalDateTime.now(clock);
        MediaCleanupPolicy cleanupPolicy = mediaPolicyProvider.getCleanupPolicy();
        List<MediaAsset> staleTempAssets = mediaAssetRepository.findAllByStatusAndCreatedAtBefore(
            MediaAssetStatus.TEMP,
            now.minus(cleanupPolicy.tempRetention())
        );
        List<MediaAsset> staleReleasedAssets = mediaAssetRepository.findAllByStatusAndUpdatedAtBefore(
            MediaAssetStatus.RELEASED,
            now.minus(cleanupPolicy.releasedRetention())
        );

        for (MediaAsset asset : staleTempAssets) {
            cleanupAsset(asset, now);
        }
        for (MediaAsset asset : staleReleasedAssets) {
            cleanupAsset(asset, now);
        }
    }

    private boolean isReferenced(Long mediaAssetId) {
        return postInternalFacade.isMediaAssetReferenced(mediaAssetId)
            || userInternalFacade.isProfilePictureReferenced(mediaAssetId);
    }

    private void cleanupAsset(MediaAsset asset, LocalDateTime now) {
        if (asset.getStatus() == MediaAssetStatus.DELETED) {
            return;
        }
        if (isReferenced(asset.getId())) {
            asset.attach(mediaUrlResolver.resolveUploadUrl(asset), now);
            log.info("[MediaAsset] cleanup skipped because asset is still referenced mediaId={}", asset.getId());
            return;
        }
        if (hasReferencedSibling(asset)) {
            log.info("[MediaAsset] cleanup skipped because sibling asset still references same key mediaId={}, key={}",
                asset.getId(), asset.getStorageKey());
            asset.release();
            return;
        }
        deleteStoredObjects(asset);
        asset.markDeleted(now);
        log.info("[MediaAsset] cleanup deleted mediaId={}, key={}", asset.getId(), asset.getStorageKey());
    }

    private boolean hasReferencedSibling(MediaAsset asset) {
        List<MediaAsset> siblings = mediaAssetRepository.findAllByStorageKeyAndIdNotAndStatusNot(
            asset.getStorageKey(),
            asset.getId(),
            MediaAssetStatus.DELETED
        );
        for (MediaAsset sibling : siblings) {
            if (isReferenced(sibling.getId())) {
                return true;
            }
        }
        return false;
    }

    private void validateRequestedMediaIds(MediaCategory category, List<Long> mediaIds) {
        MediaCategoryPolicy policy = mediaPolicyProvider.getCategoryPolicy(category);
        int maxCount = policy.maxCount();
        if (mediaIds.size() > maxCount) {
            throw MediaAssetErrorCode.TOO_MANY_MEDIA_ASSETS.defaultException();
        }
        Set<Long> uniqueIds = new HashSet<>(mediaIds);
        if (uniqueIds.size() != mediaIds.size()) {
            throw MediaAssetErrorCode.DUPLICATE_MEDIA_ASSET.defaultException();
        }
    }

    private void cleanupStoredKeys(List<String> storedKeys) {
        for (String storedKey : storedKeys) {
            try {
                fileStorageService.delete(storedKey);
            } catch (RuntimeException deleteEx) {
                log.warn("[MediaAsset] rollback cleanup failed key={}", storedKey, deleteEx);
            }
        }
    }

    private void deleteStoredObjects(MediaAsset asset) {
        if (asset.getStorageProvider() == FileStorageProvider.LEGACY) {
            mediaAssetVariantRepository.deleteAllByMediaAssetId(asset.getId());
            return;
        }

        Set<String> keysToDelete = new LinkedHashSet<>();
        if (asset.getStorageKey() != null) {
            keysToDelete.add(asset.getStorageKey());
        }
        for (MediaAssetVariant variant : mediaAssetVariantRepository.findAllByMediaAssetId(asset.getId())) {
            keysToDelete.add(variant.getStorageKey());
        }
        for (String key : keysToDelete) {
            if (key == null || key.isBlank()) {
                continue;
            }
            fileStorageService.delete(key);
        }
        mediaAssetVariantRepository.deleteAllByMediaAssetId(asset.getId());
    }

    private void triggerAsyncVariantProcessing(List<MediaAsset> assets) {
        List<Long> assetIds = assets.stream()
            .map(MediaAsset::getId)
            .toList();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            assetIds.forEach(mediaVariantProcessingService::processAsync);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                assetIds.forEach(mediaVariantProcessingService::processAsync);
            }
        });
    }
}
