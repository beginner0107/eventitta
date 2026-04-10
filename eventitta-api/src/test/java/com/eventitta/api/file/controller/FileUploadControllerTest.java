package com.eventitta.api.file.controller;

import com.eventitta.domain.file.api.internal.facade.FileStorageFacade;
import com.eventitta.domain.file.api.internal.view.StoredFileView;
import com.eventitta.domain.media.api.internal.facade.MediaAssetInternalFacade;
import com.eventitta.domain.media.api.internal.view.UploadedMediaView;
import com.eventitta.domain.media.domain.MediaAssetStatus;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.media.domain.MediaProcessingStatus;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Mock
    private MediaAssetInternalFacade mediaAssetService;

    @Mock
    private FileStorageFacade storageService;

    private FileUploadController controller;

    @BeforeEach
    void setUp() {
        controller = new FileUploadController(mediaAssetService, storageService);
    }

    @Test
    @DisplayName("인증된 사용자가 카테고리와 함께 업로드하면 미디어 자산 응답을 반환한다")
    void givenMultipartFiles_whenUpload_thenReturnsUploadedMediaResponses() {
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "test.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-jpeg".getBytes(StandardCharsets.UTF_8)
        );

        when(mediaAssetService.upload(eq(42L), eq(MediaCategory.POST_IMAGE), anyList()))
            .thenReturn(List.of(new UploadedMediaView(
                101L,
                "/api/v1/uploads/post-image/42/2026/03/26/test.jpg",
                "image/jpeg",
                1234L,
                MediaAssetStatus.TEMP,
                MediaProcessingStatus.PENDING
            )));

        ResponseEntity<List<UploadedMediaView>> response = controller.upload(42L, MediaCategory.POST_IMAGE, List.of(file));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        UploadedMediaView body = response.getBody().get(0);
        assertThat(body.mediaId()).isEqualTo(101L);
        assertThat(body.publicUrl()).isEqualTo("/api/v1/uploads/post-image/42/2026/03/26/test.jpg");
        assertThat(body.contentType()).isEqualTo("image/jpeg");
        assertThat(body.sizeBytes()).isEqualTo(1234L);
        assertThat(body.status()).isEqualTo(MediaAssetStatus.TEMP);
        assertThat(body.processingStatus()).isEqualTo(MediaProcessingStatus.PENDING);
    }

    @Test
    @DisplayName("파일 이름으로 조회 요청 시 파일을 리소스로 반환한다")
    void givenFilename_whenGet_thenReturnsFileResource() {
        String fileKey = "2024/01/15/test.txt";
        byte[] contentBytes = "file-content".getBytes(StandardCharsets.UTF_8);

        when(storageService.load(fileKey))
            .thenReturn(new StoredFileView("test.txt", MediaType.TEXT_PLAIN_VALUE, contentBytes));

        ResponseEntity<org.springframework.core.io.Resource> response = controller.serveFile(fileKey);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .isEqualTo(ContentDisposition.inline().filename("test.txt", StandardCharsets.UTF_8).build().toString());
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(response.getBody()).isInstanceOf(ByteArrayResource.class);
        assertThat(((ByteArrayResource) response.getBody()).getByteArray()).isEqualTo(contentBytes);
    }
}
