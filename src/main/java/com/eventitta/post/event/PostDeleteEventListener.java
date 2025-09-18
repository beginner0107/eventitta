package com.eventitta.post.event;

import com.eventitta.file.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class PostDeleteEventListener {

    private final FileStorageService fileStorageService;

    public PostDeleteEventListener(
        FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostDeleted(PostDeletedEvent event) {
        for (String url : event.getImageUrls()) {
            try {
                fileStorageService.delete(url);
            } catch (Exception e) {
                log.error("파일 삭제 실패: {}", url, e);
            }
        }
    }
}
