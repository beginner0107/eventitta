package com.eventitta.file.service;

import com.eventitta.file.exception.FileStorageErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
public class FileValidationService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw FileStorageErrorCode.INVALID_FILE_REQUEST.defaultException();
        }

        if (files.size() > 5) {
            throw FileStorageErrorCode.TOO_MANY_FILES.defaultException();
        }

        for (MultipartFile file : files) {
            validateSingleFile(file);
        }
    }

    private void validateSingleFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw FileStorageErrorCode.EMPTY_FILE.defaultException();
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw FileStorageErrorCode.FILE_TOO_LARGE.defaultException();
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw FileStorageErrorCode.UNSUPPORTED_FILE_TYPE.defaultException();
        }

        String filename = file.getOriginalFilename();
        if (filename != null && (filename.contains("..") || filename.contains("/"))) {
            throw FileStorageErrorCode.INVALID_FILENAME.defaultException();
        }
    }

    public Set<String> getAllowedContentTypes() {
        return ALLOWED_CONTENT_TYPES;
    }
}
