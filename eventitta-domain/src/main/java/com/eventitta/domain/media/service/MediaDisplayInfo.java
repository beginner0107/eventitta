package com.eventitta.domain.media.service;

import com.eventitta.domain.media.domain.MediaProcessingStatus;

public record MediaDisplayInfo(
    String imageUrl,
    String thumbnailUrl,
    Integer width,
    Integer height,
    MediaProcessingStatus processingStatus
) {
}
