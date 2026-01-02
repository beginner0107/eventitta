package com.eventitta.post.scheduler;

import com.eventitta.file.service.FileStorageService;
import com.eventitta.post.domain.PostImage;
import com.eventitta.post.repository.PostImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostImageFileScheduler {

    private final FileStorageService fileStorageService;
    private final PostImageRepository postImageRepository;

    /**
     * 매주 일요일 새벽 4시에 미사용 이미지 파일 정리
     * 스토리지 전체 스캔 및 DB 미등록 파일 삭제
     */
    @Scheduled(cron = "0 0 4 * * SUN", zone = "Asia/Seoul")
    @SchedulerLock(name = "deleteUnusedImageFiles", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void deleteUnusedImageFiles() {
        log.info("[Scheduler] 미사용 이미지 파일 정리 시작");

        try {
            List<PostImage> images = postImageRepository.findAll();
            List<String> imageUrls = images.stream()
                .map(PostImage::getImageUrl)
                .map(this::extractFilenameFromUrl)
                .toList();

            List<String> filesInStorage = fileStorageService.listAllFiles();
            List<String> removeFiles = filesInStorage.stream()
                .filter(file -> !imageUrls.contains(file))
                .toList();

            int deletedCount = 0;
            for (String file : removeFiles) {
                fileStorageService.delete(file);
                deletedCount++;
            }

            log.info("[Scheduler] 미사용 이미지 파일 정리 완료 - 삭제 건수: {}, 전체 파일: {}, DB 이미지: {}",
                deletedCount, filesInStorage.size(), imageUrls.size());
        } catch (Exception e) {
            log.error("[Scheduler] 미사용 이미지 파일 정리 실패", e);
        }
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl.contains("/")) {
            return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        }
        return fileUrl;
    }
}
