package com.eventitta.post.event;

import com.eventitta.file.exception.FileStorageErrorCode;
import com.eventitta.file.exception.FileStorageException;
import com.eventitta.file.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostDeleteEventListenerTest {
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PostDeleteEventListener listener;

    @Test
    @DisplayName("게시글 삭제 시 실제 이미지 파일을 전부 삭제합니다.")
    void givenPostDeletedEvent_whenOnPostDeleted_thenDeleteImages() {
        // given
        String image1 = "image1.jpg";
        String image2 = "image2.jpg";
        List<String> imageUrls = List.of(image1, image2);
        PostDeletedEvent event = new PostDeletedEvent(imageUrls);

        // when
        listener.onPostDeleted(event);

        // then
        verify(fileStorageService).delete(image1);
        verify(fileStorageService).delete(image2);
    }

    @Test
    @DisplayName("파일 삭제 실패 시에도 다른 파일 삭제는 계속 진행된다.")
    void givenFileDeleteFail_whenOnPostDeleted_thenContinueDelete() {
        // given
        String image1 = "image1.jpg";
        String image2 = "image2.jpg";
        List<String> imageUrls = List.of(image1, image2);
        PostDeletedEvent event = new PostDeletedEvent(imageUrls);

        doThrow(FileStorageException.class).when(fileStorageService)
            .delete(image1);

        // when
        listener.onPostDeleted(event);

        // then
        verify(fileStorageService).delete(image1);
        verify(fileStorageService).delete(image2);
    }
}
