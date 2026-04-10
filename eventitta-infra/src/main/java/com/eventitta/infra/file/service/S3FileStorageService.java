package com.eventitta.infra.file.service;

import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.api.internal.facade.FileStorageFacade;
import com.eventitta.domain.file.api.internal.view.StoredFileView;
import com.eventitta.domain.media.domain.MediaStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.eventitta.domain.file.exception.FileStorageErrorCode.*;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageFacade {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

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

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(content));
            return key;
        } catch (S3Exception | SdkClientException e) {
            log.error("S3 바이트 업로드 실패: key={}", key, e);
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

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.contentType())
                .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(file.content()));
            log.info("파일 업로드 완료: {}", filename);
            return key;
        } catch (S3Exception | SdkClientException e) {
            log.error("S3 업로드 실패: key={}, originalFilename={}", key, originalFilename, e);
            throw FILE_SAVE_FAIL.defaultException(e);
        }
    }

    @Override
    public StoredFileView load(String key) {
        try {
            String normalizedKey = normalizeKey(key);
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(normalizedKey)
                .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);

            String contentType = Optional.ofNullable(responseBytes.response().contentType())
                .orElse("application/octet-stream");
            return new StoredFileView(
                java.nio.file.Paths.get(normalizedKey).getFileName().toString(),
                contentType,
                responseBytes.asByteArray()
            );
        } catch (NoSuchKeyException e) {
            throw FILE_NOT_FOUND.defaultException();
        } catch (S3Exception | SdkClientException e) {
            log.error("S3 다운로드 실패: key={}", key, e);
            throw FILE_LOAD_FAIL.defaultException(e);
        }
    }

    @Override
    public byte[] readBytes(String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(normalizeKey(key))
                    .build())
                .asByteArray();
        } catch (NoSuchKeyException e) {
            throw FILE_NOT_FOUND.defaultException();
        } catch (S3Exception | SdkClientException e) {
            log.error("S3 바이트 조회 실패: key={}", key, e);
            throw FILE_LOAD_FAIL.defaultException(e);
        }
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName).key(normalizeKey(key)).build());
            log.info("S3 파일 삭제 완료: {}", key);
        } catch (NoSuchKeyException e) {
            log.warn("이미 없음: {}", key);
        } catch (S3Exception | SdkClientException e) {
            log.error("S3 파일 삭제 실패: {}", key, e);
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
    public MediaStorageProvider getStorageProvider() {
        return MediaStorageProvider.S3;
    }

    private String extractSafeExt(String name) {
        int index = name.lastIndexOf('.');
        if (index <= 0) {
            return "";
        }
        return name.substring(index).toLowerCase();
    }
}
