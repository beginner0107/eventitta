package com.eventitta.domain.media.service;

import com.eventitta.domain.file.api.internal.facade.FileStorageFacade;
import com.eventitta.domain.media.domain.MediaAsset;
import com.eventitta.domain.media.domain.MediaAssetVariant;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.media.domain.MediaProcessingStatus;
import com.eventitta.domain.media.domain.MediaVariantType;
import com.eventitta.domain.media.policy.MediaPolicyProvider;
import com.eventitta.domain.media.repository.MediaAssetVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MediaUrlResolver {

    private final FileStorageFacade fileStorageService;
    private final MediaAssetVariantRepository mediaAssetVariantRepository;
    private final MediaPolicyProvider mediaPolicyProvider;

    public String resolveOriginalUrl(String storageKey) {
        return fileStorageService.buildPublicUrl(storageKey);
    }

    public String resolveVariantUrl(String storageKey) {
        String normalizedKey = fileStorageService.normalizeKey(storageKey);
        if (!StringUtils.hasText(normalizedKey)) {
            normalizedKey = storageKey;
        }
        String cdnBaseUrl = trimTrailingSlash(mediaPolicyProvider.getDeliveryPolicy().cdnBaseUrl());
        if (StringUtils.hasText(cdnBaseUrl)) {
            return cdnBaseUrl + "/" + normalizedKey;
        }
        return fileStorageService.buildPublicUrl(normalizedKey);
    }

    public String resolveUploadUrl(MediaAsset asset) {
        if (asset == null) {
            return null;
        }
        if (asset.getProcessingStatus() == MediaProcessingStatus.READY) {
            MediaDisplayInfo displayInfo = resolveDisplayInfo(asset);
            if (StringUtils.hasText(displayInfo.imageUrl())) {
                return displayInfo.imageUrl();
            }
        }
        return fallbackOriginalUrl(asset);
    }

    public MediaDisplayInfo resolveDisplayInfo(MediaAsset asset) {
        if (asset == null) {
            return new MediaDisplayInfo(null, null, null, null, MediaProcessingStatus.FAILED);
        }
        List<MediaAssetVariant> variants = mediaAssetVariantRepository.findAllByMediaAssetId(asset.getId());
        return resolveDisplayInfo(asset, variants);
    }

    public Map<Long, MediaDisplayInfo> resolveDisplayInfos(Collection<MediaAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> assetIds = assets.stream()
            .map(MediaAsset::getId)
            .toList();
        Map<Long, List<MediaAssetVariant>> variantsByAssetId = mediaAssetVariantRepository.findAllByMediaAssetIdIn(assetIds).stream()
            .collect(Collectors.groupingBy(variant -> variant.getMediaAsset().getId()));

        Map<Long, MediaDisplayInfo> results = new LinkedHashMap<>();
        for (MediaAsset asset : assets) {
            results.put(asset.getId(), resolveDisplayInfo(asset, variantsByAssetId.getOrDefault(asset.getId(), List.of())));
        }
        return results;
    }

    public MediaDisplayInfo resolveDisplayInfo(MediaAsset asset, List<MediaAssetVariant> variants) {
        if (asset.getProcessingStatus() != MediaProcessingStatus.READY && (variants == null || variants.isEmpty())) {
            return new MediaDisplayInfo(null, null, asset.getWidth(), asset.getHeight(), asset.getProcessingStatus());
        }

        Map<MediaVariantType, MediaAssetVariant> variantMap = new EnumMap<>(MediaVariantType.class);
        for (MediaAssetVariant variant : variants) {
            variantMap.put(variant.getVariantType(), variant);
        }

        if (asset.getCategory() == MediaCategory.PROFILE_IMAGE) {
            MediaAssetVariant avatar = variantMap.get(MediaVariantType.AVATAR);
            return new MediaDisplayInfo(
                avatar != null ? avatar.getPublicUrl() : fallbackReadyUrl(asset),
                null,
                avatar != null ? avatar.getWidth() : asset.getWidth(),
                avatar != null ? avatar.getHeight() : asset.getHeight(),
                asset.getProcessingStatus()
            );
        }

        MediaAssetVariant detail = variantMap.get(MediaVariantType.DETAIL);
        MediaAssetVariant thumb = variantMap.get(MediaVariantType.THUMB);
        String imageUrl = detail != null ? detail.getPublicUrl() : fallbackReadyUrl(asset);
        String thumbnailUrl = thumb != null ? thumb.getPublicUrl() : imageUrl;

        return new MediaDisplayInfo(
            imageUrl,
            thumbnailUrl,
            detail != null ? detail.getWidth() : asset.getWidth(),
            detail != null ? detail.getHeight() : asset.getHeight(),
            asset.getProcessingStatus()
        );
    }

    private String fallbackOriginalUrl(MediaAsset asset) {
        if (StringUtils.hasText(asset.getPublicUrl())) {
            return asset.getPublicUrl();
        }
        return resolveOriginalUrl(asset.getStorageKey());
    }

    private String fallbackReadyUrl(MediaAsset asset) {
        if (StringUtils.hasText(asset.getPublicUrl())) {
            return asset.getPublicUrl();
        }
        return resolveOriginalUrl(asset.getStorageKey());
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String candidate = value.trim();
        while (candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate;
    }
}
