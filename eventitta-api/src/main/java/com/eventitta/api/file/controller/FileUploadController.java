package com.eventitta.api.file.controller;

import com.eventitta.api.common.security.annotation.CurrentUser;
import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.api.internal.facade.FileStorageFacade;
import com.eventitta.domain.file.api.internal.view.StoredFileView;
import com.eventitta.domain.file.exception.FileStorageErrorCode;
import com.eventitta.domain.media.api.internal.facade.MediaAssetInternalFacade;
import com.eventitta.domain.media.api.internal.view.UploadedMediaView;
import com.eventitta.domain.media.domain.MediaCategory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "파일 업로드 API")
public class FileUploadController {

    private final MediaAssetInternalFacade mediaAssetService;
    private final FileStorageFacade storageService;

    @Operation(summary = "파일 업로드")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<UploadedMediaView>> upload(
        @CurrentUser Long userId,
        @RequestParam("category") MediaCategory category,
        @RequestPart("files") List<MultipartFile> files
    ) {
        List<UploadFileCommand> commands = files.stream()
            .map(this::toUploadFileCommand)
            .toList();
        return ResponseEntity.ok(mediaAssetService.upload(userId, category, commands));
    }

    @Operation(summary = "업로드된 파일 조회")
    @GetMapping("/{*key}")
    public ResponseEntity<Resource> serveFile(@PathVariable("key") String key) {
        StoredFileView response = storageService.load(key);
        var builder = ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(response.filename(), StandardCharsets.UTF_8)
                .build()
                .toString())
            .contentType(resolveMediaType(response.contentType()))
            .contentLength(response.sizeBytes());
        return builder.body(new ByteArrayResource(response.content()));
    }

    private UploadFileCommand toUploadFileCommand(MultipartFile file) {
        try {
            return new UploadFileCommand(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes()
            );
        } catch (IOException e) {
            throw FileStorageErrorCode.FILE_LOAD_FAIL.defaultException(e);
        }
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }
}
