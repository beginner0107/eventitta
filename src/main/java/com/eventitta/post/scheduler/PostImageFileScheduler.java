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

    @Scheduled(cron = "0 0 3 * * SUN")
    @SchedulerLock(name = "deleteUnusedImageFiles", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void deleteUnusedImageFiles() {
        List<PostImage> images = postImageRepository.findAll();
        List<String> imageUrls = images.stream()
            .map(PostImage::getImageUrl)
            .map(this::extractFilenameFromUrl)
            .toList();

        List<String> filesInStorage = fileStorageService.listAllFiles();
        List<String> removeFiles = filesInStorage.stream()
            .filter(file -> !imageUrls.contains(file))
            .toList();

        for (String file : removeFiles) {
            fileStorageService.delete(file);
        }
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl.contains("/")) {
            return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        }
        return fileUrl;
    }
}
