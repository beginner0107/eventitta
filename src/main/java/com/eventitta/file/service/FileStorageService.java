package com.eventitta.file.service;

import com.eventitta.file.dto.response.FileDownloadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    String store(MultipartFile file);

    List<String> storeFiles(List<MultipartFile> files);

    FileDownloadResponse loadFileForDownload(String filename);

    void delete(String fileUrl);

    Resource loadAsResource(String filename);

    List<String> listAllFiles();
}
