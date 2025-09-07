package com.eventitta.file.controller;

import com.eventitta.file.dto.internal.FileDownloadResponse;
import com.eventitta.file.service.FileStorageService;
import com.eventitta.file.service.FileValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "파일 업로드 API")
public class FileUploadController {

    private final FileStorageService storageService;
    private final FileValidationService validationService;

    @Operation(summary = "파일 업로드")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> upload(@RequestPart("files") List<MultipartFile> files) {
        validationService.validateFiles(files);
        List<String> urls = storageService.storeFiles(files);
        return ResponseEntity.ok(urls);
    }

    @Operation(summary = "업로드된 파일 조회")
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable("filename") String filename) {
        FileDownloadResponse response = storageService.loadFileForDownload(filename);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, response.getContentDispositionHeader())
            .contentType(response.mediaType())
            .body(response.resource());
    }
}
