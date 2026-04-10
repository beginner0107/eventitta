package com.eventitta.domain.media.api.internal.view;

import com.eventitta.domain.media.domain.MediaAssetStatus;
import com.eventitta.domain.media.domain.MediaProcessingStatus;

public record UploadedMediaView(
    Long mediaId,
    String publicUrl,
    String contentType,
    long sizeBytes,
    MediaAssetStatus status,
    MediaProcessingStatus processingStatus
) {
}
