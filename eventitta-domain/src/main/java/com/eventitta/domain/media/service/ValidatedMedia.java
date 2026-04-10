package com.eventitta.domain.media.service;

public record ValidatedMedia(
    String originalFilename,
    String contentType,
    long sizeBytes,
    String checksum,
    int width,
    int height
) {
}
