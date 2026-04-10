package com.eventitta.infra.media.config;

import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.media.policy.MediaCategoryPolicy;
import com.eventitta.domain.media.policy.MediaCleanupPolicy;
import com.eventitta.domain.media.policy.MediaDeliveryPolicy;
import com.eventitta.domain.media.policy.MediaPolicyProvider;
import com.eventitta.domain.media.policy.MediaVariantPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfiguredMediaPolicyProvider implements MediaPolicyProvider {

    private final MediaProperties properties;

    @Override
    public MediaCategoryPolicy getCategoryPolicy(MediaCategory category) {
        MediaProperties.Category source = switch (category) {
            case POST_IMAGE -> properties.getUpload().getPostImage();
            case PROFILE_IMAGE -> properties.getUpload().getProfileImage();
        };
        return new MediaCategoryPolicy(
            source.getMaxCount(),
            source.getMaxFileSize() != null ? source.getMaxFileSize().toBytes() : 0L
        );
    }

    @Override
    public MediaCleanupPolicy getCleanupPolicy() {
        return new MediaCleanupPolicy(
            properties.getCleanup().getTempRetention(),
            properties.getCleanup().getReleasedRetention()
        );
    }

    @Override
    public MediaDeliveryPolicy getDeliveryPolicy() {
        return new MediaDeliveryPolicy(properties.getDelivery().getCdnBaseUrl());
    }

    @Override
    public MediaVariantPolicy getVariantPolicy() {
        return new MediaVariantPolicy(
            properties.getVariant().getPostThumbLongEdge(),
            properties.getVariant().getPostDetailLongEdge(),
            properties.getVariant().getProfileAvatarSize(),
            properties.getVariant().getWebpQuality()
        );
    }
}
