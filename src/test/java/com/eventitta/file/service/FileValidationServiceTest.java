package com.eventitta.file.service;

import com.eventitta.file.exception.FileStorageErrorCode;
import com.eventitta.common.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("파일 형식 유효성 검사하는 서비스 테스트")
class FileValidationServiceTest {

    private FileValidationService fileValidationService;

    @BeforeEach
    void setUp() {
        fileValidationService = new FileValidationService();
        ReflectionTestUtils.setField(fileValidationService, "maxFileSize", DataSize.ofMegabytes(5));
    }

    @Test
    @DisplayName("허용된 형식의 이미지 파일들을 업로드하면 검증을 통과한다.")
    void validateFiles_ValidFiles_Success() {
        // given
        MockMultipartFile validFile1 = new MockMultipartFile(
            "file1", "test1.jpg", "image/jpeg", "test content".getBytes()
        );
        MockMultipartFile validFile2 = new MockMultipartFile(
            "file2", "test2.png", "image/png", "test content".getBytes()
        );
        List<MultipartFile> files = List.of(validFile1, validFile2);

        // when & then
        assertDoesNotThrow(() -> fileValidationService.validateFiles(files));
    }

    @Test
    @DisplayName("파일 목록이 null이면 요청이 잘못되었다는 오류를 반환한다.")
    void validateFiles_NullFiles_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFiles(null))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.INVALID_FILE_REQUEST);
    }

    @Test
    @DisplayName("파일 목록이 비어 있으면 요청이 잘못되었다는 오류를 반환한다.")
    void validateFiles_EmptyFiles_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFiles(Collections.emptyList()))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.INVALID_FILE_REQUEST);
    }

    @Test
    @DisplayName("허용 개수를 초과해 업로드하면 ‘파일 개수 초과’ 오류를 반환한다.")
    void validateFiles_TooManyFiles_ThrowsException() {
        // given
        List<MultipartFile> files = List.of(
            createValidFile("file1.jpg"),
            createValidFile("file2.jpg"),
            createValidFile("file3.jpg"),
            createValidFile("file4.jpg"),
            createValidFile("file5.jpg"),
            createValidFile("file6.jpg")
        );

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFiles(files))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.TOO_MANY_FILES);
    }

    @Test
    @DisplayName("파일 크기가 0Byte이면 ‘빈 파일’ 오류를 반환한다.")
    void validateSingleFile_EmptyFile_ThrowsException() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", new byte[0]
        );

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFiles(List.of(emptyFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.EMPTY_FILE);
    }

    @Test
    @DisplayName("최대 크기를 초과한 파일을 업로드하면 ‘파일 크기 초과’ 오류를 반환한다.")
    void validateSingleFile_FileTooLarge_ThrowsException() {
        // given
        byte[] largeContent = new byte[(int) (6 * 1024 * 1024)]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", largeContent
        );

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFiles(List.of(largeFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("허용하지 않은 콘텐츠 타입의 파일이면 ‘지원하지 않는 파일 형식’ 오류를 반환한다.")
    void validateSingleFile_UnsupportedFileType_ThrowsException() {
        // given
        MockMultipartFile unsupportedFile = new MockMultipartFile(
            "file", "test.txt", "text/plain", "test content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFiles(List.of(unsupportedFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    @DisplayName("콘텐츠 타입이 비어 있으면 ‘지원하지 않는 파일 형식’ 오류를 반환한다.")
    void validateSingleFile_NullContentType_ThrowsException() {
        // given
        MockMultipartFile nullContentTypeFile = new MockMultipartFile(
            "file", "test.jpg", null, "test content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFiles(List.of(nullContentTypeFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    @DisplayName("경로 구분자(/)가 포함된 파일명은 ‘유효하지 않은 파일명’ 오류를 반환한다.")
    void validateSingleFile_DangerousFilename_ThrowsException() {
        // given
        MockMultipartFile dangerousFile = new MockMultipartFile(
            "file", "../test.jpg", "image/jpeg", "test content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFiles(List.of(dangerousFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.INVALID_FILENAME);
    }

    @Test
    @DisplayName("Windows 경로 구분자(\\)가 포함된 파일명은 ‘유효하지 않은 파일명’ 오류를 반환한다.")
    void validateSingleFile_FilenameWithWindowsPath_ThrowsException() {
        // given
        MockMultipartFile pathFile = new MockMultipartFile(
            "file", "folder\\test.jpg", "image/jpeg", "test content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFiles(List.of(pathFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.INVALID_FILENAME);
    }

    @Test
    @DisplayName("Windows 경로 구분자(\\)가 포함된 파일명은 ‘유효하지 않은 파일명’ 오류를 반환한다.")
    void validateSingleFile_FilenameWithPath_ThrowsException() {
        // given
        MockMultipartFile pathFile = new MockMultipartFile(
            "file", "folder/test.jpg", "image/jpeg", "test content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFiles(List.of(pathFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.INVALID_FILENAME);
    }

    @Test
    @DisplayName("허용되는 콘텐츠 타입 목록은 JPEG/PNG/GIF/PDF를 포함한다.")
    void getAllowedContentTypes_ReturnsCorrectTypes() {
        // when
        Set<String> allowedTypes = fileValidationService.getAllowedContentTypes();

        // then
        assertThat(allowedTypes).containsExactlyInAnyOrder(
            "image/jpeg", "image/png", "image/gif", "application/pdf"
        );
    }

    @Test
    @DisplayName("허용되는 모든 콘텐츠 타입(JPEG/PNG/GIF/PDF)은 검증을 통과한다.")
    void validateFiles_AllAllowedTypes_Success() {
        // given
        List<MultipartFile> files = List.of(
            new MockMultipartFile("file1", "test.jpg", "image/jpeg", "content".getBytes()),
            new MockMultipartFile("file2", "test.png", "image/png", "content".getBytes()),
            new MockMultipartFile("file3", "test.gif", "image/gif", "content".getBytes()),
            new MockMultipartFile("file4", "test.pdf", "application/pdf", "content".getBytes())
        );

        // when & then
        assertDoesNotThrow(() -> fileValidationService.validateFiles(files));
    }

    private MockMultipartFile createValidFile(String filename) {
        return new MockMultipartFile(
            "file", filename, "image/jpeg", "test content".getBytes()
        );
    }
}
