package com.eventitta.domain.media.service;

import com.eventitta.domain.file.api.internal.facade.FileStorageFacade;
import com.eventitta.domain.media.domain.MediaAsset;
import com.eventitta.domain.media.domain.MediaAssetStatus;
import com.eventitta.domain.media.domain.MediaAssetVariant;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.media.domain.MediaProcessingStatus;
import com.eventitta.domain.media.domain.MediaStorageProvider;
import com.eventitta.domain.media.domain.MediaVariantType;
import com.eventitta.domain.media.policy.MediaCategoryPolicy;
import com.eventitta.domain.media.policy.MediaCleanupPolicy;
import com.eventitta.domain.media.policy.MediaDeliveryPolicy;
import com.eventitta.domain.media.policy.MediaPolicyProvider;
import com.eventitta.domain.media.policy.MediaVariantPolicy;
import com.eventitta.domain.media.repository.MediaAssetVariantRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaUrlResolver 단위 테스트")
class MediaUrlResolverTest {

    @Mock
    private FileStorageFacade fileStorageService;

    @Mock
    private MediaAssetVariantRepository mediaAssetVariantRepository;

    @Test
    @DisplayName("CDN base URL이 설정된 운영 환경에서는 detail variant를 CDN URL로 반환한다")
    void resolveDisplayInfo_returnsCdnUrlWhenConfigured() {
        MediaUrlResolver resolver = new MediaUrlResolver(
            fileStorageService,
            mediaAssetVariantRepository,
            new TestMediaPolicyProvider("https://cdn.eventitta.com")
        );

        MediaAsset asset = MediaAsset.builder()
            .ownerUserId(1L)
            .category(MediaCategory.POST_IMAGE)
            .storageProvider(MediaStorageProvider.S3)
            .storageKey("media/original/post-image/1/2026/03/26/original.png")
            .publicUrl("/api/v1/uploads/media/original/post-image/1/2026/03/26/original.png")
            .originalFilename("original.png")
            .contentType("image/png")
            .sizeBytes(10L)
            .checksum("checksum")
            .width(1280)
            .height(720)
            .status(MediaAssetStatus.ATTACHED)
            .processingStatus(MediaProcessingStatus.READY)
            .build();
        ReflectionTestUtils.setField(asset, "id", 99L);

        MediaAssetVariant detailVariant = MediaAssetVariant.ready(
            asset,
            MediaVariantType.DETAIL,
            "media/public/detail/post-image/2026/03/26/detail.webp",
            "https://cdn.eventitta.com/media/public/detail/post-image/2026/03/26/detail.webp",
            "webp",
            1280,
            720,
            100L
        );

        when(mediaAssetVariantRepository.findAllByMediaAssetId(99L)).thenReturn(List.of(detailVariant));

        MediaDisplayInfo info = resolver.resolveDisplayInfo(asset);

        assertThat(info.imageUrl()).isEqualTo("https://cdn.eventitta.com/media/public/detail/post-image/2026/03/26/detail.webp");
    }

    private record TestMediaPolicyProvider(String cdnBaseUrl) implements MediaPolicyProvider {

        @Override
        public MediaCategoryPolicy getCategoryPolicy(MediaCategory category) {
            return new MediaCategoryPolicy(10, 5_000_000L);
        }

        @Override
        public MediaCleanupPolicy getCleanupPolicy() {
            return new MediaCleanupPolicy(Duration.ofHours(1), Duration.ofHours(1));
        }

        @Override
        public MediaDeliveryPolicy getDeliveryPolicy() {
            return new MediaDeliveryPolicy(cdnBaseUrl);
        }

        @Override
        public MediaVariantPolicy getVariantPolicy() {
            return new MediaVariantPolicy(320, 1280, 256, 0.85f);
        }
    }
}
