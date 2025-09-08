package com.eventitta.file.service;

import com.eventitta.file.dto.internal.FileDownloadResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.eventitta.file.exception.FileStorageErrorCode.*;

@Slf4j
@Service
@Profile("!prod")
public class LocalFileStorageService implements FileStorageService {

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

    public List<String> storeFiles(List<MultipartFile> files) {
        return files.stream()
            .map(this::store)
            .toList();
    }

    @Override
    public String store(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String ext = extractSafeExt(originalFileName);
        String datePath = LocalDate.now(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = UUID.randomUUID() + ext;

        Path target = rootLocation.resolve(datePath).resolve(filename).normalize();
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return (datePath + "/" + filename).replace("\\", "/");
        } catch (IOException e) {
            throw FILE_SAVE_FAIL.defaultException(e);
        }
    }

    @Override
    public FileDownloadResponse loadFileForDownload(String key) {
        Path file = safeResolve(key);
        Resource resource;
        try {
            resource = new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw FILE_LOAD_FAIL.defaultException(e);
        }
        if (!resource.exists() || !resource.isReadable()) {
            throw FILE_NOT_FOUND.defaultException();
        }

        String contentType = null;
        try {
            contentType = Files.probeContentType(file);
        } catch (IOException ex) {
            log.warn("probeContentType 실패: key={}", key, ex);
        }
        if (contentType == null) {
            contentType = MediaTypeFactory
                .getMediaType(file.getFileName().toString())
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        }

        Long len = null;
        try {
            len = Files.size(file);
        } catch (IOException ignore) {
        }

        return new FileDownloadResponse(
            resource,
            MediaType.parseMediaType(contentType),
            file.getFileName().toString(),
            len
        );
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) return;
        Path target = safeResolve(key);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw FILE_DELETE_FAIL.defaultException(e);
        }
    }

    @Override
    public Resource loadAsResource(String key) {
        try {
            Path file = safeResolve(key);
            Resource res = new UrlResource(file.toUri());
            if (res.exists() && res.isReadable()) return res;
            throw FILE_NOT_FOUND.defaultException();
        } catch (MalformedURLException e) {
            throw FILE_LOAD_FAIL.defaultException(e);
        }
    }

    @Override
    public List<String> listAllFiles() {
        if (!Files.exists(rootLocation)) return Collections.emptyList();
        try (var walk = Files.walk(rootLocation)) {
            return walk.filter(Files::isRegularFile)
                .map(p -> rootLocation.relativize(p).toString().replace("\\", "/"))
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("파일 목록 조회 실패", e);
            return Collections.emptyList();
        }
    }

    private Path safeResolve(String key) {
        String cleaned = key.replace("\\", "/").trim();
        while (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        if (cleaned.isEmpty() || cleaned.contains("..")) {
            throw FILE_NOT_FOUND.defaultException();
        }

        Path p = rootLocation.resolve(cleaned).normalize();
        if (!p.startsWith(rootLocation)) throw FILE_NOT_FOUND.defaultException();
        return p;
    }

    private String extractSafeExt(String name) {
        int index = name.lastIndexOf('.');
        if (index <= 0) return "";
        return name.substring(index).toLowerCase();
    }
}
