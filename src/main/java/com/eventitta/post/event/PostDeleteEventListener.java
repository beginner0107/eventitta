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
        if (event.getImageUrls().isEmpty()) {
            return;
        }

        log.info("[Event] PostDeleted 처리 시작 - 이미지 파일 삭제: {} 건", event.getImageUrls().size());

        int successCount = 0;
        int failCount = 0;

        for (String url : event.getImageUrls()) {
            try {
                fileStorageService.delete(url);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("[이미지 파일 삭제 실패] url={}, error={}", url, e.getMessage(), e);
            }
        }

        log.info("[Event] PostDeleted 처리 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }
}
