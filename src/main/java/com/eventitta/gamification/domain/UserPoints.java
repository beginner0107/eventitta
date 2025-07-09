package com.eventitta.gamification.domain;

import com.eventitta.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoints {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private int points;

    public UserPoints(User user) {
        this.user = user;
        this.userId = user.getId();
        this.points = 0;
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    public void subtractPoints(int amount) {
        this.points = Math.max(0, this.points - amount);
    }
}
