package com.eventitta.domain.media.policy;

import com.eventitta.domain.media.domain.MediaCategory;

public interface MediaPolicyProvider {

    MediaCategoryPolicy getCategoryPolicy(MediaCategory category);

    MediaCleanupPolicy getCleanupPolicy();

    MediaDeliveryPolicy getDeliveryPolicy();

    MediaVariantPolicy getVariantPolicy();
}
