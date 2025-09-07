package com.eventitta.file.service;

import com.eventitta.file.dto.internal.FileDownloadResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
        String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx > 0) {
            ext = original.substring(idx);
        }
        String filename = UUID.randomUUID().toString() + ext;
        try {
            Path target = rootLocation.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw FILE_SAVE_FAIL.defaultException(e);
        }
    }

    @Override
    public FileDownloadResponse loadFileForDownload(String filename) {
        Resource resource = loadAsResource(filename);
        String contentType = determineContentType(resource);

        return new FileDownloadResponse(
            resource,
            MediaType.parseMediaType(contentType),
            filename
        );
    }

    @Override
    public void delete(String fileUrl) {
        if (StringUtils.hasText(fileUrl)) {
            String filename = Paths.get(fileUrl).getFileName().toString();
            try {
                Path target = rootLocation.resolve(filename);
                Files.deleteIfExists(target);
            } catch (IOException e) {
                throw FILE_DELETE_FAIL.defaultException(e);
            }
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            throw FILE_NOT_FOUND.defaultException();
        } catch (MalformedURLException e) {
            throw FILE_LOAD_FAIL.defaultException(e);
        }
    }

    @Override
    public List<String> listAllFiles() {
        try {
            Path uploadPath = Paths.get(storageLocation);
            if (!Files.exists(uploadPath)) {
                return Collections.emptyList();
            }

            return Files.walk(uploadPath)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("파일 목록 조회 실패", e);
            return Collections.emptyList();
        }
    }

    private String determineContentType(Resource resource) {
        try {
            String ct = Files.probeContentType(resource.getFile().toPath());
            return ct != null ? ct : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
