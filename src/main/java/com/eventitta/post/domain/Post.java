package com.eventitta.post.domain;

import com.eventitta.common.config.BaseEntity;
import com.eventitta.region.domain.Region;
import com.eventitta.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false)
    private boolean deleted = false;

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
}
