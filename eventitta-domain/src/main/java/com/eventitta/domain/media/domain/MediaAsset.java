package com.eventitta.domain.media.domain;

import com.eventitta.domain.common.domain.BaseEntity;
import com.eventitta.domain.file.api.internal.FileStorageProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "media_asset",
    indexes = {
        @Index(name = "idx_media_asset_owner_status", columnList = "owner_user_id,status"),
        @Index(name = "idx_media_asset_status_created_at", columnList = "status,created_at"),
        @Index(name = "idx_media_asset_status_updated_at", columnList = "status,updated_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaAsset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 30)
    private FileStorageProvider storageProvider;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "public_url", nullable = false, length = 1024)
    private String publicUrl;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(length = 64)
    private String checksum;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaAssetStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private MediaProcessingStatus processingStatus;

    @Column(name = "processing_error", length = 500)
    private String processingError;

    @Column(name = "attached_at")
    private LocalDateTime attachedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "mediaAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MediaAssetVariant> variants = new ArrayList<>();

    @Builder
    private MediaAsset(
        Long ownerUserId,
        MediaCategory category,
        FileStorageProvider storageProvider,
        String storageKey,
        String publicUrl,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksum,
        Integer width,
        Integer height,
        MediaAssetStatus status,
        MediaProcessingStatus processingStatus,
        String processingError,
        LocalDateTime attachedAt,
        LocalDateTime deletedAt
    ) {
        this.ownerUserId = ownerUserId;
        this.category = category;
        this.storageProvider = storageProvider;
        this.storageKey = storageKey;
        this.publicUrl = publicUrl;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksum = checksum;
        this.width = width;
        this.height = height;
        this.status = status;
        this.processingStatus = processingStatus;
        this.processingError = processingError;
        this.attachedAt = attachedAt;
        this.deletedAt = deletedAt;
    }

    public static MediaAsset temp(
        Long ownerUserId,
        MediaCategory category,
        FileStorageProvider storageProvider,
        String storageKey,
        String publicUrl,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksum,
        Integer width,
        Integer height
    ) {
        return MediaAsset.builder()
            .ownerUserId(ownerUserId)
            .category(category)
            .storageProvider(storageProvider)
            .storageKey(storageKey)
            .publicUrl(publicUrl)
            .originalFilename(originalFilename)
            .contentType(contentType)
            .sizeBytes(sizeBytes)
            .checksum(checksum)
            .width(width)
            .height(height)
            .status(MediaAssetStatus.TEMP)
            .processingStatus(MediaProcessingStatus.PENDING)
            .build();
    }

    public boolean isOwnedBy(Long userId) {
        return ownerUserId != null && ownerUserId.equals(userId);
    }

    public boolean isAttachable() {
        return status == MediaAssetStatus.TEMP || status == MediaAssetStatus.ATTACHED;
    }

    public void attach(String publicUrl, LocalDateTime now) {
        this.publicUrl = publicUrl;
        this.status = MediaAssetStatus.ATTACHED;
        this.attachedAt = now;
        this.deletedAt = null;
    }

    public void release() {
        if (status == MediaAssetStatus.DELETED) {
            return;
        }
        this.status = MediaAssetStatus.RELEASED;
    }

    public void markDeleted(LocalDateTime now) {
        this.status = MediaAssetStatus.DELETED;
        this.deletedAt = now;
    }

    public void markProcessing() {
        this.processingStatus = MediaProcessingStatus.PROCESSING;
        this.processingError = null;
    }

    public void markReady(String publicUrl) {
        this.processingStatus = MediaProcessingStatus.READY;
        this.processingError = null;
        if (publicUrl != null) {
            this.publicUrl = publicUrl;
        }
    }

    public void markFailed(String errorMessage) {
        this.processingStatus = MediaProcessingStatus.FAILED;
        this.processingError = errorMessage;
    }

    public void addVariant(MediaAssetVariant variant) {
        variants.add(variant);
    }
}
