package com.eventitta.post.domain;

import com.eventitta.common.domain.BaseEntity;
import com.eventitta.region.domain.Region;
import com.eventitta.user.domain.User;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_code", nullable = false)
    private Region region;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PostImage> images = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostLike> likes = new HashSet<>();

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    public Post(User user, String title, String content, Region region) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.region = region;
    }

    public static Post create(
        User user,
        String title,
        String content,
        Region region
    ) {
        return new Post(user, title, content, region);
    }

    public void update(String title, String content, Region region) {
        this.title = title;
        this.content = content;
        this.region = region;
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
}
