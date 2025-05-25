package com.eventitta.file.controller;

import com.eventitta.common.storage.FileStorageService;
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

    @Operation(summary = "파일 업로드")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> upload(
        @RequestPart("files") List<MultipartFile> files
    ) {
        List<String> urls = files.stream()
            .map(storageService::store)
            .toList();
        return ResponseEntity.ok(urls);
    }

    @Operation(summary = "업로드된 파일 조회")
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable("filename") String filename) {
        Resource resource = storageService.loadAsResource(filename);
        String contentType = determineContentType(resource);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
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
