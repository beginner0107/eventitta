package com.eventitta.domain.post.domain;

import com.eventitta.domain.common.domain.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "post_likes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class PostLike extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected PostLike() {
    }

    public PostLike(Post post, Long userId) {
        this.post = post;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public Long getUserId() {
        return userId;
    }
}
