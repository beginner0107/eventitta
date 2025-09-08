package com.eventitta.file.dto.internal;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record FileDownloadResponse(
    Resource resource,
    MediaType mediaType,
    String filename,
    Long contentLength
) {
    public String getContentDispositionHeader() {
        try {
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
            return String.format("inline; filename*=UTF-8''%s", encodedFilename);
        } catch (Exception e) {
            return String.format("inline; filename=\"%s\"", filename);
        }
    }
}
