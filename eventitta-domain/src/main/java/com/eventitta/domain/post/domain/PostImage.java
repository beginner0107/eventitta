package com.eventitta.domain.post.domain;

import com.eventitta.domain.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_image")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "media_asset_id", nullable = false)
    private Long mediaAssetId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Setter
    @Column(nullable = false)
    private int sortOrder;

    public PostImage(String imageUrl, int sortOrder) {
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    public PostImage(Long mediaAssetId, String imageUrl, int sortOrder) {
        this.mediaAssetId = mediaAssetId;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }
}
