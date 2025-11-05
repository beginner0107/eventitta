package com.eventitta.file.service;

import com.eventitta.file.dto.response.FileDownloadResponse;
import com.eventitta.common.exception.CustomException;
import com.eventitta.file.exception.FileStorageErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3FileStorageService – S3 기반 파일 저장/조회/삭제 정책을 검증한다.")
class S3FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Utilities s3Utilities;

    private S3FileStorageService s3FileStorageService;

    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        s3FileStorageService = new S3FileStorageService(s3Client);
        ReflectionTestUtils.setField(s3FileStorageService, "bucketName", bucketName);
    }

    @Test
    @DisplayName("단일 파일을 업로드하면 S3에 저장되고 키를 반환한다.")
    void store_SingleFile_Success() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        // when
        String key = s3FileStorageService.store(file);

        // then
        assertThat(key).isNotNull();
        assertThat(key).contains("/");
        assertThat(key).endsWith(".jpg");

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("여러 파일을 업로드하면 각 파일에 대한 키를 반환한다.")
    void storeFiles_MultipleFiles_Success() {
        // given
        List<MultipartFile> files = List.of(
            new MockMultipartFile("file1", "test1.jpg", "image/jpeg", "content1".getBytes()),
            new MockMultipartFile("file2", "test2.png", "image/png", "content2".getBytes())
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        // when
        List<String> keys = s3FileStorageService.storeFiles(files);

        // then
        assertThat(keys).hasSize(2);
        keys.forEach(key -> {
            assertThat(key).isNotNull();
            assertThat(key).contains("/");
        });

        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3에서 오류가 발생하면 업로드는 실패하고 FILE_SAVE_FAIL 오류를 반환한다.")
    void store_S3Exception_ThrowsException() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder().message("S3 Error").build());

        // when & then
        assertThatThrownBy(() -> s3FileStorageService.store(file))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_SAVE_FAIL);
    }

    @Test
    @DisplayName("SDK 클라이언트 오류가 발생하면 업로드는 실패하고 FILE_SAVE_FAIL 오류를 반환한다.")
    void store_SdkClientException_ThrowsException() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(SdkClientException.create("SDK Error"));

        // when & then
        assertThatThrownBy(() -> s3FileStorageService.store(file))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_SAVE_FAIL);
    }

    @Test
    @DisplayName("존재하는 키로 다운로드하면 리소스와 콘텐츠 타입, 파일명을 포함한 응답을 반환한다.")
    void loadFileForDownload_ExistingFile_Success() {
        // given
        String key = "2025/01/01/test.jpg";
        byte[] content = "test content".getBytes();

        GetObjectResponse response = GetObjectResponse.builder()
            .contentType("image/jpeg")
            .contentLength((long) content.length)
            .build();

        ResponseInputStream<GetObjectResponse> responseInputStream =
            new ResponseInputStream<>(response, new ByteArrayInputStream(content));

        when(s3Client.getObject(any(GetObjectRequest.class)))
            .thenReturn(responseInputStream);

        // when
        FileDownloadResponse downloadResponse = s3FileStorageService.loadFileForDownload(key);

        // then
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.resource()).isNotNull();
        assertThat(downloadResponse.mediaType().toString()).isEqualTo("image/jpeg");
        assertThat(downloadResponse.contentLength()).isEqualTo(content.length);
        assertThat(downloadResponse.filename()).isEqualTo("test.jpg");
    }

    @Test
    @DisplayName("존재하지 않는 키로 다운로드하면 FILE_NOT_FOUND 오류를 반환한다.")
    void loadFileForDownload_NonExistentFile_ThrowsException() {
        // given
        String key = "2025/01/01/nonexistent.jpg";

        when(s3Client.getObject(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder().build());

        // when & then
        assertThatThrownBy(() -> s3FileStorageService.loadFileForDownload(key))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("다운로드 중 S3 오류가 발생하면 FILE_LOAD_FAIL 오류를 반환한다.")
    void loadFileForDownload_S3Exception_ThrowsException() {
        // given
        String key = "2025/01/01/test.jpg";

        when(s3Client.getObject(any(GetObjectRequest.class)))
            .thenThrow(S3Exception.builder().message("S3 Error").build());

        // when & then
        assertThatThrownBy(() -> s3FileStorageService.loadFileForDownload(key))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_LOAD_FAIL);
    }

    @Test
    @DisplayName("존재하는 키로 삭제를 요청하면 S3 객체가 제거된다.")
    void delete_ExistingFile_Success() {
        // given
        String key = "2025/01/01/test.jpg";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenReturn(DeleteObjectResponse.builder().build());

        // when
        s3FileStorageService.delete(key);

        // then
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("존재하지 않는 키를 삭제하면 예외 없이 넘어가고 경고 로그만 남는다.")
    void delete_NonExistentFile_LogsWarning() {
        // given
        String key = "2025/01/01/nonexistent.jpg";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder().build());

        // when & then
        assertDoesNotThrow(() -> s3FileStorageService.delete(key));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("삭제 중 S3 오류가 발생하면 FILE_DELETE_FAIL 오류를 반환한다.")
    void delete_S3Exception_ThrowsException() {
        // given
        String key = "2025/01/01/test.jpg";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenThrow(S3Exception.builder().message("S3 Error").build());

        // when & then
        assertThatThrownBy(() -> s3FileStorageService.delete(key))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_DELETE_FAIL);
    }

    @Test
    @DisplayName("빈 키나 null 키로 삭제를 요청하면 아무 작업도 하지 않는다.")
    void delete_EmptyKey_DoesNothing() {
        // when & then
        assertDoesNotThrow(() -> s3FileStorageService.delete(""));
        assertDoesNotThrow(() -> s3FileStorageService.delete(null));

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("존재하는 키를 리소스로 요청하면 S3 URL을 가진 리소스를 반환한다.")
    void loadAsResource_ExistingFile_Success() throws Exception {
        // given
        String key = "2025/01/01/test.jpg";
        URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/" + key);

        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(Consumer.class))).thenReturn(mockUrl);

        // when
        Resource resource = s3FileStorageService.loadAsResource(key);

        // then
        assertThat(resource).isNotNull();
        assertThat(resource.getURL()).isEqualTo(mockUrl);

        verify(s3Client).utilities();
        verify(s3Utilities).getUrl(any(Consumer.class));
    }

    @Test
    @DisplayName("URL 생성이 실패하면 FILE_NOT_FOUND 오류를 반환한다.")
    void loadAsResource_UrlGenerationFails_ThrowsException() {
        // given
        String key = "2025/01/01/test.jpg";

        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(Consumer.class))).thenThrow(new RuntimeException("URL 생성 실패"));

        // when & then
        assertThatThrownBy(() -> s3FileStorageService.loadAsResource(key))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_NOT_FOUND);

        verify(s3Client).utilities();
        verify(s3Utilities).getUrl(any(Consumer.class));
    }

    @Test
    @DisplayName("파일 목록이 한 페이지에 있으면 모든 키를 반환한다.")
    void listAllFiles_SinglePage_Success() {
        // given
        S3Object obj1 = S3Object.builder().key("2025/01/01/file1.jpg").build();
        S3Object obj2 = S3Object.builder().key("2025/01/01/file2.png").build();

        ListObjectsV2Response response = ListObjectsV2Response.builder()
            .contents(obj1, obj2)
            .isTruncated(false)
            .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenReturn(response);

        // when
        List<String> files = s3FileStorageService.listAllFiles();

        // then
        assertThat(files).hasSize(2);
        assertThat(files).containsExactlyInAnyOrder(
            "2025/01/01/file1.jpg", "2025/01/01/file2.png"
        );
    }

    @Test
    @DisplayName("파일 목록이 여러 페이지로 나뉘어 있으면 페이지를 이어 받아 모든 키를 반환한다.")
    void listAllFiles_MultiplePages_Success() {
        // given
        S3Object obj1 = S3Object.builder().key("2025/01/01/file1.jpg").build();
        S3Object obj2 = S3Object.builder().key("2025/01/01/file2.png").build();

        ListObjectsV2Response response1 = ListObjectsV2Response.builder()
            .contents(obj1)
            .isTruncated(true)
            .nextContinuationToken("token1")
            .build();

        ListObjectsV2Response response2 = ListObjectsV2Response.builder()
            .contents(obj2)
            .isTruncated(false)
            .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenReturn(response1, response2);

        // when
        List<String> files = s3FileStorageService.listAllFiles();

        // then
        assertThat(files).hasSize(2);
        assertThat(files).containsExactlyInAnyOrder(
            "2025/01/01/file1.jpg", "2025/01/01/file2.png"
        );
        verify(s3Client, times(2)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    @DisplayName("파일 목록 조회 중 S3 오류가 발생하면 빈 리스트를 반환한다.")
    void listAllFiles_S3Exception_ReturnsEmptyList() {
        // given
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenThrow(S3Exception.builder().message("S3 Error").build());

        // when
        List<String> files = s3FileStorageService.listAllFiles();

        // then
        assertThat(files).isEmpty();
    }

    @Test
    @DisplayName("대문자 확장자는 소문자로 정규화되어 저장된다.")
    void extractSafeExt_UppercaseExtension_ReturnsLowercase() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.JPEG", "image/jpeg", "content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        // when
        String key = s3FileStorageService.store(file);

        // then
        assertThat(key).endsWith(".jpeg");
    }

    @Test
    @DisplayName("확장자가 없는 파일은 확장자 없이 저장된다.")
    void extractSafeExt_NoExtension_ReturnsEmpty() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "testfile", "image/jpeg", "content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        // when
        String key = s3FileStorageService.store(file);

        // then
        assertThat(key).isNotNull();
        assertThat(key).doesNotEndWith(".");
    }
}
