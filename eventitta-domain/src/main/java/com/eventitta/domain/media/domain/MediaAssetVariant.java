package com.eventitta.domain.media.domain;

import com.eventitta.domain.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "media_asset_variant",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_media_asset_variant_type", columnNames = {"media_asset_id", "variant_type"})
    },
    indexes = {
        @Index(name = "idx_media_asset_variant_asset_status", columnList = "media_asset_id,status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaAssetVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_asset_id", nullable = false)
    private MediaAsset mediaAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type", nullable = false, length = 30)
    private MediaVariantType variantType;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "public_url", nullable = false, length = 1024)
    private String publicUrl;

    @Column(nullable = false, length = 20)
    private String format;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaProcessingStatus status;

    @Builder
    private MediaAssetVariant(
        MediaAsset mediaAsset,
        MediaVariantType variantType,
        String storageKey,
        String publicUrl,
        String format,
        Integer width,
        Integer height,
        long sizeBytes,
        MediaProcessingStatus status
    ) {
        this.mediaAsset = mediaAsset;
        this.variantType = variantType;
        this.storageKey = storageKey;
        this.publicUrl = publicUrl;
        this.format = format;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.status = status;
    }

    public static MediaAssetVariant ready(
        MediaAsset mediaAsset,
        MediaVariantType variantType,
        String storageKey,
        String publicUrl,
        String format,
        Integer width,
        Integer height,
        long sizeBytes
    ) {
        return MediaAssetVariant.builder()
            .mediaAsset(mediaAsset)
            .variantType(variantType)
            .storageKey(storageKey)
            .publicUrl(publicUrl)
            .format(format)
            .width(width)
            .height(height)
            .sizeBytes(sizeBytes)
            .status(MediaProcessingStatus.READY)
            .build();
    }
}
