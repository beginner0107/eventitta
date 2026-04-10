package com.eventitta.domain.media.service;

import com.eventitta.domain.file.api.internal.facade.FileStorageFacade;
import com.eventitta.domain.media.domain.*;
import com.eventitta.domain.media.repository.MediaAssetRepository;
import com.eventitta.domain.media.repository.MediaAssetVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaVariantProcessingService {

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAssetVariantRepository mediaAssetVariantRepository;
    private final FileStorageFacade fileStorageService;
    private final MediaImageProcessor mediaImageProcessor;
    private final MediaUrlResolver mediaUrlResolver;

    @Async("mediaExecutor")
    public void processAsync(Long mediaAssetId) {
        processInNewTransaction(mediaAssetId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processInNewTransaction(Long mediaAssetId) {
        doProcess(mediaAssetId);
    }

    @Transactional
    public void process(Long mediaAssetId) {
        doProcess(mediaAssetId);
    }

    private void doProcess(Long mediaAssetId) {
        MediaAsset asset = mediaAssetRepository.findById(mediaAssetId).orElse(null);
        if (asset == null || asset.getProcessingStatus() == MediaProcessingStatus.READY) {
            return;
        }

        asset.markProcessing();
        mediaAssetVariantRepository.deleteAllByMediaAssetId(asset.getId());

        List<String> storedVariantKeys = new ArrayList<>();
        try {
            byte[] originalBytes = fileStorageService.readBytes(asset.getStorageKey());
            List<MediaAssetVariant> variants = new ArrayList<>();
            for (MediaVariantType variantType : variantTypesFor(asset.getCategory())) {
                MediaImageProcessor.MediaRenderedVariant rendered = mediaImageProcessor.render(asset.getCategory(), variantType, originalBytes);
                String variantKey = fileStorageService.storeBytes(
                    rendered.content(),
                    "media/public/" + variantType.name().toLowerCase() + "/" + asset.getCategory().storageDirectory(),
                    "image/webp",
                    ".webp"
                );
                storedVariantKeys.add(variantKey);
                String publicUrl = mediaUrlResolver.resolveVariantUrl(variantKey);
                variants.add(MediaAssetVariant.ready(
                    asset,
                    variantType,
                    variantKey,
                    publicUrl,
                    rendered.format(),
                    rendered.width(),
                    rendered.height(),
                    rendered.content().length
                ));
            }

            for (MediaAssetVariant variant : variants) {
                asset.addVariant(variant);
            }
            mediaAssetVariantRepository.saveAllAndFlush(variants);
            asset.markReady(mediaUrlResolver.resolveDisplayInfo(asset, variants).imageUrl());
            mediaAssetRepository.flush();
            log.info("[MediaAsset] variant processing complete mediaId={}, variantCount={}", asset.getId(), variants.size());
        } catch (RuntimeException ex) {
            cleanupVariantKeys(storedVariantKeys);
            asset.markFailed(truncateError(ex.getMessage()));
            mediaAssetRepository.flush();
            log.error("[MediaAsset] variant processing failed mediaId={}", asset.getId(), ex);
        }
    }

    private Set<MediaVariantType> variantTypesFor(MediaCategory category) {
        return switch (category) {
            case POST_IMAGE -> EnumSet.of(MediaVariantType.THUMB, MediaVariantType.DETAIL);
            case PROFILE_IMAGE -> EnumSet.of(MediaVariantType.AVATAR);
        };
    }

    private void cleanupVariantKeys(List<String> variantKeys) {
        for (String variantKey : variantKeys) {
            try {
                fileStorageService.delete(variantKey);
            } catch (RuntimeException e) {
                log.warn("[MediaAsset] failed to cleanup variant key={}", variantKey, e);
            }
        }
    }

    private String truncateError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
