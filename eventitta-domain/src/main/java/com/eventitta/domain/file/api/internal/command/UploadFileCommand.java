package com.eventitta.domain.file.api.internal.command;

public record UploadFileCommand(
    String filename,
    String contentType,
    long sizeBytes,
    byte[] content
) {
}
