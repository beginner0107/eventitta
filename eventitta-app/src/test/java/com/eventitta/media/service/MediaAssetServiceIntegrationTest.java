package com.eventitta.domain.media.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.api.internal.facade.FileStorageFacade;
import com.eventitta.domain.media.domain.MediaAsset;
import com.eventitta.domain.media.domain.MediaAssetVariant;
import com.eventitta.domain.media.domain.MediaAssetStatus;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.media.domain.MediaProcessingStatus;
import com.eventitta.domain.media.domain.MediaStorageProvider;
import com.eventitta.domain.media.domain.MediaVariantType;
import com.eventitta.domain.media.api.internal.view.UploadedMediaView;
import com.eventitta.domain.media.exception.MediaAssetException;
import com.eventitta.domain.media.repository.MediaAssetRepository;
import com.eventitta.domain.media.repository.MediaAssetVariantRepository;
import com.eventitta.domain.user.domain.Provider;
import com.eventitta.domain.user.domain.Role;
import com.eventitta.domain.user.domain.User;
import com.eventitta.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static com.eventitta.domain.media.exception.MediaAssetErrorCode.MEDIA_ASSET_ACCESS_DENIED;
import static com.eventitta.domain.media.exception.MediaAssetErrorCode.TOO_MANY_MEDIA_ASSETS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Transactional
@DisplayName("미디어 자산 서비스 통합 테스트")
class MediaAssetServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MediaVariantProcessingService mediaVariantProcessingService;

    @Autowired
    private MediaAssetVariantRepository mediaAssetVariantRepository;

    @MockitoBean
    private FileStorageFacade fileStorageService;

    @Test
    @DisplayName("업로드하면 TEMP 상태의 미디어 자산이 생성되고 응답이 반환된다")
    void upload_createsTempMediaAsset() {
        User owner = userRepository.saveAndFlush(createUser("media-upload@test.com", "mediaUpload"));
        UploadFileCommand file = new UploadFileCommand(
            "post.png",
            "image/png",
            pngBytes().length,
            pngBytes()
        );

        when(fileStorageService.store(any(UploadFileCommand.class), eq("media/original/post-image/" + owner.getId())))
            .thenReturn("media/original/post-image/" + owner.getId() + "/2026/03/26/post.png");
        when(fileStorageService.buildPublicUrl(anyString()))
            .thenAnswer(invocation -> "/api/v1/uploads/" + invocation.getArgument(0));
        when(fileStorageService.getStorageProvider()).thenReturn(MediaStorageProvider.LOCAL);

        List<UploadedMediaView> responses = mediaAssetService.upload(owner.getId(), MediaCategory.POST_IMAGE, List.of(file));

        assertThat(responses).hasSize(1);
        UploadedMediaView response = responses.get(0);
        assertThat(response.publicUrl()).isEqualTo("/api/v1/uploads/media/original/post-image/" + owner.getId() + "/2026/03/26/post.png");
        assertThat(response.status()).isEqualTo(MediaAssetStatus.TEMP);
        assertThat(response.processingStatus()).isEqualTo(MediaProcessingStatus.PENDING);

        MediaAsset persisted = mediaAssetRepository.findById(response.mediaId()).orElseThrow();
        assertThat(persisted.getOwnerUserId()).isEqualTo(owner.getId());
        assertThat(persisted.getCategory()).isEqualTo(MediaCategory.POST_IMAGE);
        assertThat(persisted.getStatus()).isEqualTo(MediaAssetStatus.TEMP);
        assertThat(persisted.getProcessingStatus()).isEqualTo(MediaProcessingStatus.PENDING);
        assertThat(persisted.getWidth()).isEqualTo(1);
        assertThat(persisted.getHeight()).isEqualTo(1);
    }

    @Test
    @DisplayName("파생본 처리에 성공하면 READY 상태와 variant 레코드가 생성된다")
    void process_createsVariantsAndMarksReady() {
        User owner = userRepository.saveAndFlush(createUser("media-ready@test.com", "mediaReady"));
        MediaAsset asset = mediaAssetRepository.saveAndFlush(MediaAsset.temp(
            owner.getId(),
            MediaCategory.POST_IMAGE,
            MediaStorageProvider.LOCAL,
            "media/original/post-image/" + owner.getId() + "/2026/03/26/original.png",
            "/api/v1/uploads/media/original/post-image/" + owner.getId() + "/2026/03/26/original.png",
            "original.png",
            "image/png",
            pngBytes().length,
            "checksum",
            1,
            1
        ));

        when(fileStorageService.readBytes(asset.getStorageKey())).thenReturn(pngBytes());
        when(fileStorageService.storeBytes(any(byte[].class), eq("media/public/thumb/post-image"), eq("image/webp"), eq(".webp")))
            .thenReturn("media/public/thumb/post-image/2026/03/26/thumb.webp");
        when(fileStorageService.storeBytes(any(byte[].class), eq("media/public/detail/post-image"), eq("image/webp"), eq(".webp")))
            .thenReturn("media/public/detail/post-image/2026/03/26/detail.webp");
        when(fileStorageService.buildPublicUrl(anyString()))
            .thenAnswer(invocation -> "/api/v1/uploads/" + invocation.getArgument(0));

        mediaVariantProcessingService.process(asset.getId());
        entityManager.clear();

        MediaAsset persisted = mediaAssetRepository.findById(asset.getId()).orElseThrow();
        List<MediaAssetVariant> variants = mediaAssetVariantRepository.findAllByMediaAssetId(asset.getId());

        assertThat(persisted.getProcessingStatus()).isEqualTo(MediaProcessingStatus.READY);
        assertThat(persisted.getPublicUrl()).isEqualTo("/api/v1/uploads/media/public/detail/post-image/2026/03/26/detail.webp");
        assertThat(variants).hasSize(2);
        assertThat(variants).extracting(MediaAssetVariant::getVariantType)
            .containsExactlyInAnyOrder(MediaVariantType.THUMB, MediaVariantType.DETAIL);
    }

    @Test
    @DisplayName("다른 사용자의 TEMP 자산은 연결할 수 없다")
    void resolveAttachableAssets_rejectsForeignOwner() {
        User owner = userRepository.saveAndFlush(createUser("media-owner@test.com", "mediaOwner"));
        User otherUser = userRepository.saveAndFlush(createUser("media-other@test.com", "mediaOther"));
        MediaAsset asset = mediaAssetRepository.saveAndFlush(createTempAsset(owner, "post-image/" + owner.getId() + "/2026/03/26/asset.png"));

        assertThatThrownBy(() -> mediaAssetService.resolveAttachableAssets(otherUser.getId(), MediaCategory.POST_IMAGE, List.of(asset.getId())))
            .isInstanceOf(MediaAssetException.class)
            .extracting("errorCode")
            .isEqualTo(MEDIA_ASSET_ACCESS_DENIED);
    }

    @Test
    @DisplayName("게시글 이미지는 서비스 레벨에서도 최대 5개까지만 연결할 수 있다")
    void resolveAttachableAssets_rejectsTooManyPostImages() {
        assertThatThrownBy(() -> mediaAssetService.resolveAttachableAssets(1L, MediaCategory.POST_IMAGE, List.of(1L, 2L, 3L, 4L, 5L, 6L)))
            .isInstanceOf(MediaAssetException.class)
            .extracting("errorCode")
            .isEqualTo(TOO_MANY_MEDIA_ASSETS);
    }

    @Test
    @DisplayName("같은 storage key를 다른 활성 참조가 사용 중이면 cleanup이 물리 파일을 삭제하지 않는다")
    void cleanupStaleAssets_skipsReferencedSiblingKey() {
        User owner = userRepository.saveAndFlush(createUser("cleanup-owner@test.com", "cleanupOwner"));

        MediaAsset activeAsset = mediaAssetRepository.saveAndFlush(MediaAsset.builder()
            .ownerUserId(owner.getId())
            .category(MediaCategory.PROFILE_IMAGE)
            .storageProvider(MediaStorageProvider.LOCAL)
            .storageKey("profile-image/" + owner.getId() + "/shared-key.png")
            .publicUrl("/api/v1/uploads/profile-image/" + owner.getId() + "/shared-key.png")
            .originalFilename("shared-key.png")
            .contentType("image/png")
            .sizeBytes(1L)
            .checksum("checksum-active")
            .width(1)
            .height(1)
            .status(MediaAssetStatus.ATTACHED)
            .processingStatus(MediaProcessingStatus.READY)
            .attachedAt(LocalDateTime.now())
            .build());
        owner.updateProfile(owner.getNickname(), activeAsset.getId(), activeAsset.getPublicUrl(), null, null, null, null, null);
        userRepository.saveAndFlush(owner);

        MediaAsset releasedAsset = mediaAssetRepository.saveAndFlush(MediaAsset.builder()
            .ownerUserId(owner.getId())
            .category(MediaCategory.POST_IMAGE)
            .storageProvider(MediaStorageProvider.LOCAL)
            .storageKey(activeAsset.getStorageKey())
            .publicUrl(activeAsset.getPublicUrl())
            .originalFilename("shared-key.png")
            .contentType("image/png")
            .sizeBytes(1L)
            .checksum("checksum-released")
            .width(1)
            .height(1)
            .status(MediaAssetStatus.RELEASED)
            .processingStatus(MediaProcessingStatus.READY)
            .attachedAt(LocalDateTime.now().minusDays(10))
            .build());
        jdbcTemplate.update(
            "update media_asset set updated_at = ? where id = ?",
            Timestamp.valueOf(LocalDateTime.now().minusDays(10)),
            releasedAsset.getId()
        );

        mediaAssetService.cleanupStaleAssets();

        verify(fileStorageService, never()).delete(activeAsset.getStorageKey());
        MediaAsset persistedReleasedAsset = mediaAssetRepository.findById(releasedAsset.getId()).orElseThrow();
        assertThat(persistedReleasedAsset.getStatus()).isEqualTo(MediaAssetStatus.RELEASED);
    }

    @Test
    @DisplayName("stale RELEASED 자산 정리 시 원본과 모든 variant 키를 함께 삭제한다")
    void cleanupStaleAssets_deletesOriginalAndVariantKeys() {
        User owner = userRepository.saveAndFlush(createUser("cleanup-delete@test.com", "cleanupDelete"));
        MediaAsset releasedAsset = mediaAssetRepository.saveAndFlush(MediaAsset.builder()
            .ownerUserId(owner.getId())
            .category(MediaCategory.POST_IMAGE)
            .storageProvider(MediaStorageProvider.LOCAL)
            .storageKey("media/original/post-image/" + owner.getId() + "/2026/03/26/original.png")
            .publicUrl("/api/v1/uploads/media/public/detail/post-image/2026/03/26/detail.webp")
            .originalFilename("original.png")
            .contentType("image/png")
            .sizeBytes(10L)
            .checksum("cleanup-checksum")
            .width(100)
            .height(100)
            .status(MediaAssetStatus.RELEASED)
            .processingStatus(MediaProcessingStatus.READY)
            .attachedAt(LocalDateTime.now().minusDays(40))
            .build());
        mediaAssetVariantRepository.saveAllAndFlush(List.of(
            MediaAssetVariant.ready(
                releasedAsset,
                MediaVariantType.THUMB,
                "media/public/thumb/post-image/2026/03/26/thumb.webp",
                "/api/v1/uploads/media/public/thumb/post-image/2026/03/26/thumb.webp",
                "webp",
                320,
                320,
                100L
            ),
            MediaAssetVariant.ready(
                releasedAsset,
                MediaVariantType.DETAIL,
                "media/public/detail/post-image/2026/03/26/detail.webp",
                "/api/v1/uploads/media/public/detail/post-image/2026/03/26/detail.webp",
                "webp",
                1280,
                1280,
                200L
            )
        ));
        jdbcTemplate.update(
            "update media_asset set updated_at = ? where id = ?",
            Timestamp.valueOf(LocalDateTime.now().minusDays(40)),
            releasedAsset.getId()
        );

        mediaAssetService.cleanupStaleAssets();

        verify(fileStorageService).delete("media/original/post-image/" + owner.getId() + "/2026/03/26/original.png");
        verify(fileStorageService).delete("media/public/thumb/post-image/2026/03/26/thumb.webp");
        verify(fileStorageService).delete("media/public/detail/post-image/2026/03/26/detail.webp");
        MediaAsset persisted = mediaAssetRepository.findById(releasedAsset.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(MediaAssetStatus.DELETED);
        assertThat(mediaAssetVariantRepository.findAllByMediaAssetId(releasedAsset.getId())).isEmpty();
    }

    private User createUser(String email, String nickname) {
        return User.builder()
            .email(email)
            .password("encoded-password")
            .nickname(nickname)
            .provider(Provider.LOCAL)
            .role(Role.USER)
            .build();
    }

    private MediaAsset createTempAsset(User owner, String storageKey) {
        return MediaAsset.temp(
            owner.getId(),
            MediaCategory.POST_IMAGE,
            MediaStorageProvider.LOCAL,
            storageKey,
            "/api/v1/uploads/" + storageKey,
            "asset.png",
            "image/png",
            1L,
            "checksum",
            1,
            1
        );
    }

    private byte[] pngBytes() {
        try {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
