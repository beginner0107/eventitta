package com.eventitta.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file);

    void delete(String fileUrl);

    Resource loadAsResource(String filename);
}
