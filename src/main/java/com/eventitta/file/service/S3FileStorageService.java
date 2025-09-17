package com.eventitta.file.service;

import com.eventitta.file.dto.internal.FileDownloadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.eventitta.file.exception.FileStorageErrorCode.*;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Override
    public List<String> storeFiles(List<MultipartFile> files) {
        return files.stream()
            .map(this::store)
            .toList();
    }

    @Override
    public String store(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String ext = extractSafeExt(originalFilename);
        String datePath = LocalDate.now(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = UUID.randomUUID() + ext;
        String key = (datePath + "/" + filename).replace("\\", "/");

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

            try (InputStream in = file.getInputStream()) {
                s3Client.putObject(putRequest, RequestBody.fromInputStream(in, file.getSize()));
                log.info("파일 업로드 완료: {}", filename);
                return key;
            }
        } catch (S3Exception | SdkClientException | IOException e) {
            log.error("S3 업로드 실패: key={}, originalFilename={}", key, originalFilename, e);
            throw FILE_SAVE_FAIL.defaultException();
        }
    }

    public FileDownloadResponse loadFileForDownload(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            var s3InputStream = s3Client.getObject(request);

            String contentType = Optional.ofNullable(s3InputStream.response().contentType())
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            Long len = Optional.ofNullable(s3InputStream.response().contentLength()).orElse(0L);

            Resource resource = new InputStreamResource(s3InputStream);
            String downloadName = java.nio.file.Paths.get(key).getFileName().toString();

            return new FileDownloadResponse(
                resource,
                MediaType.parseMediaType(contentType),
                downloadName,
                len
            );

        } catch (NoSuchKeyException e) {
            throw FILE_NOT_FOUND.defaultException();
        } catch (S3Exception | SdkClientException e) {
            log.error("S3 다운로드 실패: key={}", key, e);
            throw FILE_LOAD_FAIL.defaultException(e);
        }
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName).key(key).build());
            log.info("S3 파일 삭제 완료: {}", key);
        } catch (NoSuchKeyException e) {
            log.warn("이미 없음: {}", key);
        } catch (S3Exception | SdkClientException e) {
            log.error("S3 파일 삭제 실패: {}", key, e);
            throw FILE_DELETE_FAIL.defaultException(e);
        }
    }

    @Override
    public Resource loadAsResource(String key) {
        try {
            URL url = s3Client.utilities().getUrl(b -> b.bucket(bucketName).key(key));
            return new UrlResource(url);
        } catch (Exception e) {
            log.error("S3 URL 생성 실패: key={}", key, e);
            throw FILE_NOT_FOUND.defaultException();
        }
    }

    @Override
    public List<String> listAllFiles() {
        List<String> keys = new ArrayList<>();
        String token = null;
        try {
            do {
                var req = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .continuationToken(token)
                    .build();
                var listObjectsV2Res = s3Client.listObjectsV2(req);
                listObjectsV2Res.contents().forEach(o -> keys.add(o.key()));
                token = listObjectsV2Res.isTruncated() ? listObjectsV2Res.nextContinuationToken() : null;
            } while (token != null);
            return keys;
        } catch (S3Exception | SdkClientException e) {
            log.error("S3 파일 목록 조회 실패", e);
            return Collections.emptyList();
        }
    }

    private String extractSafeExt(String name) {
        int index = name.lastIndexOf('.');
        if (index <= 0) return "";
        return name.substring(index).toLowerCase();
    }
}
