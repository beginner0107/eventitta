package com.eventitta.infra.file.service;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.api.internal.view.StoredFileView;
import com.eventitta.domain.file.exception.FileStorageErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LocalFileStorageService – 로컬 파일 저장/조회/삭제 정책을 보장한다.")
class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService localFileStorageService;

    @BeforeEach
    void setUp() {
        localFileStorageService = new LocalFileStorageService();
        ReflectionTestUtils.setField(localFileStorageService, "storageLocation", tempDir.toString());
        localFileStorageService.init();
    }

    @Test
    @DisplayName("단일 파일을 업로드하면 날짜 경로 하위에 안전한 이름으로 저장된다.")
    void store_SingleFile_Success() throws IOException {
        UploadFileCommand file = uploadFile("test.jpg", "image/jpeg", "test content".getBytes());

        String key = localFileStorageService.store(file, "legacy");

        assertThat(key).isNotNull();
        assertThat(key).contains("/");
        assertThat(key).endsWith(".jpg");

        Path savedFile = tempDir.resolve(key);
        assertTrue(Files.exists(savedFile));
        assertThat(Files.readAllBytes(savedFile)).isEqualTo("test content".getBytes());
    }

    @Test
    @DisplayName("대문자 확장자는 소문자로 정규화되어 저장된다.")
    void store_ExtractSafeExtension_Success() {
        UploadFileCommand file = uploadFile("test.JPEG", "image/jpeg", "content".getBytes());

        String key = localFileStorageService.store(file, "legacy");

        assertThat(key).endsWith(".jpeg");
    }

    @Test
    @DisplayName("확장자가 없는 파일은 확장자 없이 저장 키를 반환한다.")
    void store_FileWithoutExtension_Success() {
        UploadFileCommand file = uploadFile("testfile", "image/jpeg", "content".getBytes());

        String key = localFileStorageService.store(file, "legacy");

        assertThat(key).isNotNull();
        assertThat(key).doesNotContain(".");
    }

    @Test
    @DisplayName("존재하는 파일을 다운로드하면 파일명과 콘텐츠 타입, 길이를 포함한 응답을 반환한다.")
    void load_ExistingFile_Success() {
        UploadFileCommand file = uploadFile("test.jpg", "image/jpeg", "test content".getBytes());
        String key = localFileStorageService.store(file, "legacy");

        StoredFileView response = localFileStorageService.load(key);

        assertThat(response).isNotNull();
        assertThat(response.filename()).endsWith(".jpg");
        assertThat(response.content()).isEqualTo("test content".getBytes());
    }

    @Test
    @DisplayName("존재하지 않는 키로 다운로드를 요청하면 ‘파일을 찾을 수 없음’ 오류를 반환한다.")
    void load_NonExistentFile_ThrowsException() {
        assertThatThrownBy(() -> localFileStorageService.load("2025/01/01/nonexistent.jpg"))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하는 파일을 삭제하면 물리 파일이 제거된다.")
    void delete_ExistingFile_Success() {
        UploadFileCommand file = uploadFile("test.jpg", "image/jpeg", "content".getBytes());
        String key = localFileStorageService.store(file, "legacy");
        Path savedFile = tempDir.resolve(key);
        assertTrue(Files.exists(savedFile));

        localFileStorageService.delete(key);

        assertFalse(Files.exists(savedFile));
    }

    @Test
    @DisplayName("존재하지 않는 키를 삭제해도 정책상 예외 없이 넘어간다.")
    void delete_NonExistentFile_NoException() {
        assertDoesNotThrow(() -> localFileStorageService.delete("2025/01/01/nonexistent.jpg"));
    }

    @Test
    @DisplayName("키가 비어 있거나 null이면 삭제를 수행하지 않는다.")
    void delete_EmptyKey_DoesNothing() {
        assertDoesNotThrow(() -> localFileStorageService.delete(""));
        assertDoesNotThrow(() -> localFileStorageService.delete(null));
    }

    private UploadFileCommand uploadFile(String filename, String contentType, byte[] content) {
        return new UploadFileCommand(filename, contentType, content.length, content);
    }
}
