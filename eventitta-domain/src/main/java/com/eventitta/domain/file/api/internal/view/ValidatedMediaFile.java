package com.eventitta.domain.file.api.internal.view;

public record ValidatedMediaFile(
    String originalFilename,
    String contentType,
    long sizeBytes,
    String checksum,
    int width,
    int height
) {
}
