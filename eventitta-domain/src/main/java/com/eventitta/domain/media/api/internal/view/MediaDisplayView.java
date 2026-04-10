package com.eventitta.domain.media.api.internal.view;

import com.eventitta.domain.media.domain.MediaProcessingStatus;

public record MediaDisplayView(
    String imageUrl,
    String thumbnailUrl,
    Integer width,
    Integer height,
    MediaProcessingStatus processingStatus
) {
}
