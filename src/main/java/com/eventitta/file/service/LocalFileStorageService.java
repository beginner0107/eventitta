package com.eventitta.file.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

import static com.eventitta.file.exception.FileStorageErrorCode.*;

@Slf4j
@Service
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
            throw STORAGE_INIT_FAIL.defaultException(e);
        }
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
}
