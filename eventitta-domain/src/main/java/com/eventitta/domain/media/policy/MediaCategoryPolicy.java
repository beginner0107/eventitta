package com.eventitta.domain.media.policy;

public record MediaCategoryPolicy(
    int maxCount,
    long maxFileSizeBytes
) {
}
