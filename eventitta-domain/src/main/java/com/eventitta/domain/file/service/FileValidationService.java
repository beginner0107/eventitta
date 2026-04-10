package com.eventitta.domain.file.service;

import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.api.internal.facade.FileValidationFacade;
import com.eventitta.domain.file.api.internal.view.ValidatedMediaFile;
import com.eventitta.domain.file.exception.FileStorageErrorCode;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.media.policy.MediaCategoryPolicy;
import com.eventitta.domain.media.policy.MediaPolicyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileValidationService implements FileValidationFacade {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif"
    );

    private final MediaPolicyProvider mediaPolicyProvider;

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private DataSize fallbackMaxFileSize;

    @Override
    public List<ValidatedMediaFile> validateFiles(MediaCategory category, List<UploadFileCommand> files) {
        if (files == null || files.isEmpty()) {
            throw FileStorageErrorCode.INVALID_FILE_REQUEST.defaultException();
        }

        MediaCategoryPolicy policy = mediaPolicyProvider.getCategoryPolicy(category);
        if (files.size() > policy.maxCount()) {
            throw FileStorageErrorCode.TOO_MANY_FILES.defaultException();
        }

        return files.stream()
            .map(file -> validateSingleFile(category, file, policy))
            .toList();
    }

    private ValidatedMediaFile validateSingleFile(
        MediaCategory category,
        UploadFileCommand file,
        MediaCategoryPolicy policy
    ) {
        byte[] content = file.content() != null ? file.content() : new byte[0];
        long sizeBytes = file.sizeBytes() > 0 ? file.sizeBytes() : content.length;
        if (content.length == 0 || sizeBytes == 0) {
            throw FileStorageErrorCode.EMPTY_FILE.defaultException();
        }

        long maxFileSizeBytes = policy.maxFileSizeBytes() > 0 ? policy.maxFileSizeBytes() : fallbackMaxFileSize.toBytes();
        if (sizeBytes > maxFileSizeBytes) {
            throw FileStorageErrorCode.FILE_TOO_LARGE.defaultException();
        }

        String filename = file.filename();
        if (filename != null && (filename.contains("..") || filename.contains("/") || filename.contains("\\"))) {
            throw FileStorageErrorCode.INVALID_FILENAME.defaultException();
        }

        DetectedImage detected = detectImage(content);

        String declaredContentType = normalizeContentType(file.contentType());
        if (!StringUtils.hasText(declaredContentType)
            || !ALLOWED_IMAGE_CONTENT_TYPES.contains(declaredContentType)
            || !declaredContentType.equals(detected.contentType())
        ) {
            throw FileStorageErrorCode.UNSUPPORTED_FILE_TYPE.defaultException();
        }

        if (category == MediaCategory.PROFILE_IMAGE && sizeBytes == 0) {
            throw FileStorageErrorCode.EMPTY_FILE.defaultException();
        }

        return new ValidatedMediaFile(
            filename != null ? filename : "upload",
            declaredContentType,
            sizeBytes,
            checksum(content),
            detected.width(),
            detected.height()
        );
    }

    private DetectedImage detectImage(byte[] content) {
        String signatureContentType = detectContentTypeBySignature(content);
        if (signatureContentType == null) {
            throw FileStorageErrorCode.UNSUPPORTED_FILE_TYPE.defaultException();
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw FileStorageErrorCode.UNSUPPORTED_FILE_TYPE.defaultException();
            }
            return new DetectedImage(signatureContentType, image.getWidth(), image.getHeight());
        } catch (IOException e) {
            throw FileStorageErrorCode.UNSUPPORTED_FILE_TYPE.defaultException(e);
        }
    }

    private String detectContentTypeBySignature(byte[] content) {
        if (content.length >= 3
            && (content[0] & 0xFF) == 0xFF
            && (content[1] & 0xFF) == 0xD8
            && (content[2] & 0xFF) == 0xFF
        ) {
            return "image/jpeg";
        }
        if (content.length >= 8
            && (content[0] & 0xFF) == 0x89
            && content[1] == 0x50
            && content[2] == 0x4E
            && content[3] == 0x47
            && content[4] == 0x0D
            && content[5] == 0x0A
            && content[6] == 0x1A
            && content[7] == 0x0A
        ) {
            return "image/png";
        }
        if (content.length >= 6) {
            String header = new String(content, 0, 6);
            if ("GIF87a".equals(header) || "GIF89a".equals(header)) {
                return "image/gif";
            }
        }
        return null;
    }

    private String checksum(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw FileStorageErrorCode.FILE_LOAD_FAIL.defaultException(e);
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? null : contentType.toLowerCase(Locale.ROOT);
    }

    private record DetectedImage(String contentType, int width, int height) {
    }
}
