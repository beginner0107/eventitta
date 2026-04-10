package com.eventitta.domain.file.service;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.exception.FileStorageErrorCode;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.media.policy.MediaCategoryPolicy;
import com.eventitta.domain.media.policy.MediaCleanupPolicy;
import com.eventitta.domain.media.policy.MediaDeliveryPolicy;
import com.eventitta.domain.media.policy.MediaPolicyProvider;
import com.eventitta.domain.media.policy.MediaVariantPolicy;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("파일 형식 유효성 검사하는 서비스 테스트")
class FileValidationServiceTest {

    private FileValidationService fileValidationService;

    @BeforeEach
    void setUp() {
        fileValidationService = new FileValidationService(new TestMediaPolicyProvider());
        ReflectionTestUtils.setField(fileValidationService, "fallbackMaxFileSize", DataSize.ofMegabytes(5));
    }

    @Test
    @DisplayName("허용된 이미지 파일들을 업로드하면 검증을 통과한다.")
    void validateFiles_ValidPostImages_Success() {
        List<UploadFileCommand> files = List.of(
            imageFile("test1.png", "image/png", pngBytes(1, 1)),
            imageFile("test2.gif", "image/gif", gifBytes())
        );

        assertDoesNotThrow(() -> fileValidationService.validateFiles(MediaCategory.POST_IMAGE, files));
    }

    @Test
    @DisplayName("프로필 이미지는 1개까지만 허용된다.")
    void validateFiles_ProfileImageCountExceeded_ThrowsException() {
        List<UploadFileCommand> files = List.of(
            imageFile("profile1.png", "image/png", pngBytes(1, 1)),
            imageFile("profile2.png", "image/png", pngBytes(1, 1))
        );

        assertThatThrownBy(() -> fileValidationService.validateFiles(MediaCategory.PROFILE_IMAGE, files))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.TOO_MANY_FILES);
    }

    @Test
    @DisplayName("파일 목록이 null이면 요청이 잘못되었다는 오류를 반환한다.")
    void validateFiles_NullFiles_ThrowsException() {
        assertThatThrownBy(() -> fileValidationService.validateFiles(MediaCategory.POST_IMAGE, null))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.INVALID_FILE_REQUEST);
    }

    @Test
    @DisplayName("파일 목록이 비어 있으면 요청이 잘못되었다는 오류를 반환한다.")
    void validateFiles_EmptyFiles_ThrowsException() {
        assertThatThrownBy(() -> fileValidationService.validateFiles(MediaCategory.POST_IMAGE, Collections.emptyList()))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.INVALID_FILE_REQUEST);
    }

    @Test
    @DisplayName("최대 크기를 초과한 파일을 업로드하면 ‘파일 크기 초과’ 오류를 반환한다.")
    void validateSingleFile_FileTooLarge_ThrowsException() {
        byte[] largeContent = new byte[6 * 1024 * 1024];
        UploadFileCommand largeFile = imageFile("test.png", "image/png", largeContent);

        assertThatThrownBy(() -> fileValidationService.validateFiles(MediaCategory.POST_IMAGE, List.of(largeFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("PDF 파일은 게시글 이미지 카테고리에서 거부된다.")
    void validateSingleFile_PdfRejected_ThrowsException() {
        UploadFileCommand pdfFile = new UploadFileCommand(
            "test.pdf",
            "application/pdf",
            "%PDF-1.4".getBytes().length,
            "%PDF-1.4".getBytes()
        );

        assertThatThrownBy(() -> fileValidationService.validateFiles(MediaCategory.POST_IMAGE, List.of(pdfFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    @DisplayName("파일 시그니처와 MIME 타입이 다르면 거부된다.")
    void validateSingleFile_MismatchedMimeType_ThrowsException() {
        UploadFileCommand mismatchedFile = imageFile("test.png", "image/jpeg", pngBytes(1, 1));

        assertThatThrownBy(() -> fileValidationService.validateFiles(MediaCategory.POST_IMAGE, List.of(mismatchedFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    @DisplayName("경로 구분자(/)가 포함된 파일명은 ‘유효하지 않은 파일명’ 오류를 반환한다.")
    void validateSingleFile_DangerousFilename_ThrowsException() {
        UploadFileCommand dangerousFile = imageFile("folder/test.png", "image/png", pngBytes(1, 1));

        assertThatThrownBy(() -> fileValidationService.validateFiles(MediaCategory.POST_IMAGE, List.of(dangerousFile)))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", FileStorageErrorCode.INVALID_FILENAME);
    }

    private UploadFileCommand imageFile(String filename, String contentType, byte[] content) {
        return new UploadFileCommand(filename, contentType, content.length, content);
    }

    private byte[] pngBytes(int width, int height) {
        return imageBytes("png", width, height);
    }

    private byte[] gifBytes() {
        return imageBytes("gif", 1, 1);
    }

    private byte[] imageBytes(String format, int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, format, output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class TestMediaPolicyProvider implements MediaPolicyProvider {

        @Override
        public MediaCategoryPolicy getCategoryPolicy(MediaCategory category) {
            return switch (category) {
                case PROFILE_IMAGE -> new MediaCategoryPolicy(1, DataSize.ofMegabytes(2).toBytes());
                case POST_IMAGE -> new MediaCategoryPolicy(10, DataSize.ofMegabytes(5).toBytes());
            };
        }

        @Override
        public MediaCleanupPolicy getCleanupPolicy() {
            return new MediaCleanupPolicy(Duration.ofHours(1), Duration.ofHours(1));
        }

        @Override
        public MediaDeliveryPolicy getDeliveryPolicy() {
            return new MediaDeliveryPolicy(null);
        }

        @Override
        public MediaVariantPolicy getVariantPolicy() {
            return new MediaVariantPolicy(320, 1280, 256, 0.85f);
        }
    }
}
