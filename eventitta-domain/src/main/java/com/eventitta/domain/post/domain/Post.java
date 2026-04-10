package com.eventitta.domain.post.domain;

import com.eventitta.domain.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long authorUserId;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "region_code", nullable = false, length = 20)
    private String regionCode;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PostImage> images = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostLike> likes = new HashSet<>();

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    public Post(Long authorUserId, String title, String content, String regionCode) {
        this.authorUserId = authorUserId;
        this.title = title;
        this.content = content;
        this.regionCode = regionCode;
    }

    public static Post create(
        Long authorUserId,
        String title,
        String content,
        String regionCode
    ) {
        return new Post(authorUserId, title, content, regionCode);
    }

    public void update(String title, String content, String regionCode) {
        this.title = title;
        this.content = content;
        this.regionCode = regionCode;
    }

    public void softDelete() {
        this.deleted = true;
    }

    public void addImage(PostImage img) {
        images.add(img);
        img.setPost(this);
    }

    public void removeImage(PostImage img) {
        images.remove(img);
        img.setPost(null);
    }

    public void clearImages() {
        for (PostImage img : new ArrayList<>(images)) {
            removeImage(img);
        }
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public boolean isAuthor(Long userId) {
        return authorUserId != null && authorUserId.equals(userId);
    }
}
