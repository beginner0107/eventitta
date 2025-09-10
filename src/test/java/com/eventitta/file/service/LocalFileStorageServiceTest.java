package com.eventitta.file.service;

import com.eventitta.file.dto.internal.FileDownloadResponse;
import com.eventitta.common.exception.CustomException;
import com.eventitta.file.exception.FileStorageErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

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
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        // when
        String key = localFileStorageService.store(file);

        // then
        assertThat(key).isNotNull();
        assertThat(key).contains("/");
        assertThat(key).endsWith(".jpg");

        Path savedFile = tempDir.resolve(key);
        assertTrue(Files.exists(savedFile));
        assertThat(Files.readAllBytes(savedFile)).isEqualTo("test content".getBytes());
    }

    @Test
    @DisplayName("여러 파일을 업로드하면 각 파일에 대한 키를 반환하고 물리 파일이 생성된다.")
    void storeFiles_MultipleFiles_Success() {
        // given
        List<MultipartFile> files = List.of(
            new MockMultipartFile("file1", "test1.jpg", "image/jpeg", "content1".getBytes()),
            new MockMultipartFile("file2", "test2.png", "image/png", "content2".getBytes())
        );

        // when
        List<String> keys = localFileStorageService.storeFiles(files);

        // then
        assertThat(keys).hasSize(2);
        keys.forEach(key -> {
            assertThat(key).isNotNull();
            assertTrue(Files.exists(tempDir.resolve(key)));
        });
    }

    @Test
    @DisplayName("대문자 확장자는 소문자로 정규화되어 저장된다.")
    void store_ExtractSafeExtension_Success() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.JPEG", "image/jpeg", "content".getBytes()
        );

        // when
        String key = localFileStorageService.store(file);

        // then
        assertThat(key).endsWith(".jpeg");
    }

    @Test
    @DisplayName("확장자가 없는 파일은 확장자 없이 저장 키를 반환한다.")
    void store_FileWithoutExtension_Success() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "testfile", "image/jpeg", "content".getBytes()
        );

        // when
        String key = localFileStorageService.store(file);

        // then
        assertThat(key).isNotNull();
        assertThat(key).doesNotContain(".");
    }

    @Test
    @DisplayName("존재하는 파일을 다운로드하면 리소스·콘텐츠 타입·길이가 포함된 응답을 반환한다.")
    void loadFileForDownload_ExistingFile_Success() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes()
        );
        String key = localFileStorageService.store(file);

        // when
        FileDownloadResponse response = localFileStorageService.loadFileForDownload(key);

        // then
        assertThat(response).isNotNull();
        assertThat(response.resource()).isNotNull();
        assertThat(response.resource().exists()).isTrue();
        assertThat(response.mediaType().toString()).contains("image/jpeg");
        assertThat(response.contentLength()).isEqualTo(12L);
    }

    @Test
    @DisplayName("존재하지 않는 키로 다운로드를 요청하면 ‘파일을 찾을 수 없음’ 오류를 반환한다.")
    void loadFileForDownload_NonExistentFile_ThrowsException() {
        // given
        String nonExistentKey = "2025/01/01/nonexistent.jpg";

        // when & then
        assertThatThrownBy(() -> localFileStorageService.loadFileForDownload(nonExistentKey))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하는 파일을 삭제하면 물리 파일이 제거된다.")
    void delete_ExistingFile_Success() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes()
        );
        String key = localFileStorageService.store(file);
        Path savedFile = tempDir.resolve(key);
        assertTrue(Files.exists(savedFile));

        // when
        localFileStorageService.delete(key);

        // then
        assertFalse(Files.exists(savedFile));
    }

    @Test
    @DisplayName("존재하지 않는 키를 삭제해도 정책상 예외 없이 넘어간다.")
    void delete_NonExistentFile_NoException() {
        // given
        String nonExistentKey = "2025/01/01/nonexistent.jpg";

        // when & then
        assertDoesNotThrow(() -> localFileStorageService.delete(nonExistentKey));
    }

    @Test
    @DisplayName("키가 비어 있거나 null이면 삭제를 수행하지 않는다.")
    void delete_EmptyKey_DoesNothing() {
        // when & then
        assertDoesNotThrow(() -> localFileStorageService.delete(""));
        assertDoesNotThrow(() -> localFileStorageService.delete(null));
    }

    @Test
    @DisplayName("존재하는 키로 리소스를 로드하면 읽기 가능한 리소스를 반환한다.")
    void loadAsResource_ExistingFile_Success() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes()
        );
        String key = localFileStorageService.store(file);

        // when
        Resource resource = localFileStorageService.loadAsResource(key);

        // then
        assertThat(resource).isNotNull();
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    @DisplayName("존재하지 않는 키로 리소스를 로드하면 ‘파일을 찾을 수 없음’ 오류를 반환한다.")
    void loadAsResource_NonExistentFile_ThrowsException() {
        // given
        String nonExistentKey = "2025/01/01/nonexistent.jpg";

        // when & then
        assertThatThrownBy(() -> localFileStorageService.loadAsResource(nonExistentKey))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("파일이 존재하면 전체 목록 조회는 저장된 키들을 반환한다.")
    void listAllFiles_WithFiles_ReturnsFileList() {
        // given
        List<MultipartFile> files = List.of(
            new MockMultipartFile("file1", "test1.jpg", "image/jpeg", "content1".getBytes()),
            new MockMultipartFile("file2", "test2.png", "image/png", "content2".getBytes())
        );
        List<String> storedKeys = localFileStorageService.storeFiles(files);

        // when
        List<String> allFiles = localFileStorageService.listAllFiles();

        // then
        assertThat(allFiles).hasSize(2);
        assertThat(allFiles).containsAll(storedKeys);
    }

    @Test
    @DisplayName("파일이 없으면 전체 목록 조회는 빈 리스트를 반환한다.")
    void listAllFiles_NoFiles_ReturnsEmptyList() {
        // when
        List<String> allFiles = localFileStorageService.listAllFiles();

        // then
        assertThat(allFiles).isEmpty();
    }

    @Test
    @DisplayName("상위 경로 참조(../)가 포함된 키는 접근을 차단하고 ‘파일을 찾을 수 없음’ 오류를 반환한다.")
    void safeResolve_DangerousPath_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> localFileStorageService.loadAsResource("../dangerous.txt"))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("키가 비어 있으면 접근을 차단하고 ‘파일을 찾을 수 없음’ 오류를 반환한다.")
    void safeResolve_EmptyPath_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> localFileStorageService.loadAsResource(""))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("역슬래시(\\)를 포함한 키는 정규화되어 올바른 리소스로 해석된다.")
    void safeResolve_PathNormalization_Success() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes()
        );
        String key = localFileStorageService.store(file);

        // when
        String windowsStyleKey = key.replace("/", "\\");
        Resource resource = localFileStorageService.loadAsResource(windowsStyleKey);

        // then
        assertThat(resource).isNotNull();
        assertTrue(resource.exists());
    }
}
