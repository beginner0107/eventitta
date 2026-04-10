package com.eventitta.infra.file.service;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.api.internal.view.StoredFileView;
import com.eventitta.domain.file.exception.FileStorageErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3FileStorageService – S3 기반 파일 저장/조회/삭제 정책을 검증한다.")
class S3FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    private S3FileStorageService s3FileStorageService;

    @BeforeEach
    void setUp() {
        s3FileStorageService = new S3FileStorageService(s3Client);
        ReflectionTestUtils.setField(s3FileStorageService, "bucketName", "test-bucket");
    }

    @Test
    @DisplayName("단일 파일을 업로드하면 S3에 저장되고 키를 반환한다.")
    void store_SingleFile_Success() {
        UploadFileCommand file = uploadFile("test.jpg", "image/jpeg", "test content".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        String key = s3FileStorageService.store(file, "legacy");

        assertThat(key).isNotNull();
        assertThat(key).contains("/");
        assertThat(key).endsWith(".jpg");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3에서 오류가 발생하면 업로드는 실패하고 FILE_SAVE_FAIL 오류를 반환한다.")
    void store_S3Exception_ThrowsException() {
        UploadFileCommand file = uploadFile("test.jpg", "image/jpeg", "content".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder().message("S3 Error").build());

        assertThatThrownBy(() -> s3FileStorageService.store(file, "legacy"))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_SAVE_FAIL);
    }

    @Test
    @DisplayName("SDK 클라이언트 오류가 발생하면 업로드는 실패하고 FILE_SAVE_FAIL 오류를 반환한다.")
    void store_SdkClientException_ThrowsException() {
        UploadFileCommand file = uploadFile("test.jpg", "image/jpeg", "content".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(SdkClientException.create("SDK Error"));

        assertThatThrownBy(() -> s3FileStorageService.store(file, "legacy"))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_SAVE_FAIL);
    }

    @Test
    @DisplayName("존재하는 키로 다운로드하면 파일명과 콘텐츠 타입을 포함한 응답을 반환한다.")
    void load_ExistingFile_Success() {
        String key = "2025/01/01/test.jpg";
        byte[] content = "test content".getBytes();
        GetObjectResponse response = GetObjectResponse.builder()
            .contentType("image/jpeg")
            .build();

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromByteArray(response, content));

        StoredFileView downloadResponse = s3FileStorageService.load(key);

        assertThat(downloadResponse.filename()).isEqualTo("test.jpg");
        assertThat(downloadResponse.contentType()).isEqualTo("image/jpeg");
        assertThat(downloadResponse.content()).isEqualTo(content);
    }

    @Test
    @DisplayName("존재하지 않는 키로 다운로드하면 FILE_NOT_FOUND 오류를 반환한다.")
    void load_NonExistentFile_ThrowsException() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder().build());

        assertThatThrownBy(() -> s3FileStorageService.load("2025/01/01/nonexistent.jpg"))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("다운로드 중 S3 오류가 발생하면 FILE_LOAD_FAIL 오류를 반환한다.")
    void load_S3Exception_ThrowsException() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(S3Exception.builder().message("S3 Error").build());

        assertThatThrownBy(() -> s3FileStorageService.load("2025/01/01/test.jpg"))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_LOAD_FAIL);
    }

    @Test
    @DisplayName("존재하는 키로 삭제를 요청하면 S3 객체가 제거된다.")
    void delete_ExistingFile_Success() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenReturn(DeleteObjectResponse.builder().build());

        s3FileStorageService.delete("2025/01/01/test.jpg");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("존재하지 않는 키를 삭제하면 예외 없이 넘어간다.")
    void delete_NonExistentFile_LogsWarning() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder().build());

        assertDoesNotThrow(() -> s3FileStorageService.delete("2025/01/01/nonexistent.jpg"));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("삭제 중 S3 오류가 발생하면 FILE_DELETE_FAIL 오류를 반환한다.")
    void delete_S3Exception_ThrowsException() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenThrow(S3Exception.builder().message("S3 Error").build());

        assertThatThrownBy(() -> s3FileStorageService.delete("2025/01/01/test.jpg"))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_DELETE_FAIL);
    }

    @Test
    @DisplayName("빈 키나 null 키로 삭제를 요청하면 아무 작업도 하지 않는다.")
    void delete_EmptyKey_DoesNothing() {
        assertDoesNotThrow(() -> s3FileStorageService.delete(""));
        assertDoesNotThrow(() -> s3FileStorageService.delete(null));

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("대문자 확장자는 소문자로 정규화되어 저장된다.")
    void extractSafeExt_UppercaseExtension_ReturnsLowercase() {
        UploadFileCommand file = uploadFile("test.JPEG", "image/jpeg", "content".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        String key = s3FileStorageService.store(file, "legacy");

        assertThat(key).endsWith(".jpeg");
    }

    @Test
    @DisplayName("확장자가 없는 파일은 확장자 없이 저장된다.")
    void extractSafeExt_NoExtension_ReturnsEmpty() {
        UploadFileCommand file = uploadFile("testfile", "image/jpeg", "content".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        String key = s3FileStorageService.store(file, "legacy");

        assertThat(key).isNotNull();
        assertThat(key).doesNotEndWith(".");
    }

    private UploadFileCommand uploadFile(String filename, String contentType, byte[] content) {
        return new UploadFileCommand(filename, contentType, content.length, content);
    }
}
