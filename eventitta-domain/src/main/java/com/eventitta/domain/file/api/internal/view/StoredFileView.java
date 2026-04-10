package com.eventitta.domain.file.api.internal.view;

public record StoredFileView(
    String filename,
    String contentType,
    byte[] content
) {
    public long sizeBytes() {
        return content != null ? content.length : 0L;
    }
}
