package com.eventitta.infra.file.service;

import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.api.internal.facade.FileStorageFacade;
import com.eventitta.domain.file.api.internal.view.StoredFileView;
import com.eventitta.domain.file.api.internal.FileStorageProvider;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.eventitta.domain.file.exception.FileStorageErrorCode.FILE_DELETE_FAIL;
import static com.eventitta.domain.file.exception.FileStorageErrorCode.FILE_LOAD_FAIL;
import static com.eventitta.domain.file.exception.FileStorageErrorCode.FILE_NOT_FOUND;
import static com.eventitta.domain.file.exception.FileStorageErrorCode.FILE_SAVE_FAIL;

@Slf4j
@Service
@Profile("!prod")
public class LocalFileStorageService implements FileStorageFacade {

    @Value("${file.storage.location:uploads}")
    private String storageLocation;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw FILE_SAVE_FAIL.defaultException(e);
        }
    }

    @Override
    public String storeBytes(byte[] content, String keyPrefix, String contentType, String extension) {
        String safeExtension = extension == null ? "" : extension.trim().toLowerCase();
        if (!safeExtension.isEmpty() && !safeExtension.startsWith(".")) {
            safeExtension = "." + safeExtension;
        }

        String datePath = LocalDate.now(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = UUID.randomUUID() + safeExtension;
        String normalizedPrefix = normalizeKey(keyPrefix);
        String key = (normalizedPrefix + "/" + datePath + "/" + filename).replace("\\", "/");

        Path target = safeResolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return key;
        } catch (IOException e) {
            throw FILE_SAVE_FAIL.defaultException(e);
        }
    }

    @Override
    public String store(UploadFileCommand file, String keyPrefix) {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.filename(), "upload"));
        String ext = extractSafeExt(originalFilename);
        String datePath = LocalDate.now(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = UUID.randomUUID() + ext;
        String normalizedPrefix = normalizeKey(keyPrefix);
        String key = (normalizedPrefix + "/" + datePath + "/" + filename).replace("\\", "/");

        Path target = safeResolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, file.content());
            return key;
        } catch (IOException e) {
            throw FILE_SAVE_FAIL.defaultException(e);
        }
    }

    @Override
    public StoredFileView load(String key) {
        Path file = safeResolve(key);
        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw FILE_NOT_FOUND.defaultException();
        }

        try {
            String contentType = Files.probeContentType(file);
            return new StoredFileView(
                file.getFileName().toString(),
                contentType != null ? contentType : "application/octet-stream",
                Files.readAllBytes(file)
            );
        } catch (IOException e) {
            throw FILE_LOAD_FAIL.defaultException(e);
        }
    }

    @Override
    public byte[] readBytes(String key) {
        Path file = safeResolve(key);
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw FILE_LOAD_FAIL.defaultException(e);
        }
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        Path target = safeResolve(normalizeKey(key));
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw FILE_DELETE_FAIL.defaultException(e);
        }
    }

    @Override
    public String buildPublicUrl(String key) {
        return "/api/v1/uploads/" + normalizeKey(key);
    }

    @Override
    public String normalizeKey(String keyOrUrl) {
        return StorageKeyUtils.normalizeKey(keyOrUrl);
    }

    @Override
    public FileStorageProvider getStorageProvider() {
        return FileStorageProvider.LOCAL;
    }

    private Path safeResolve(String key) {
        String cleaned = normalizeKey(key);
        if (cleaned.isEmpty() || cleaned.contains("..")) {
            throw FILE_NOT_FOUND.defaultException();
        }

        Path path = rootLocation.resolve(cleaned).normalize();
        if (!path.startsWith(rootLocation)) {
            throw FILE_NOT_FOUND.defaultException();
        }
        return path;
    }

    private String extractSafeExt(String name) {
        int index = name.lastIndexOf('.');
        if (index <= 0) {
            return "";
        }
        return name.substring(index).toLowerCase();
    }
}
