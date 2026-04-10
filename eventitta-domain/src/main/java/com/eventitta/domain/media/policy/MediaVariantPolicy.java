package com.eventitta.domain.media.policy;

public record MediaVariantPolicy(
    int postThumbLongEdge,
    int postDetailLongEdge,
    int profileAvatarSize,
    float webpQuality
) {
}
