package com.eventitta.gamification.domain;

import com.eventitta.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "user_points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoints {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private int points = 0;

    private UserPoints(User user) {
        this.user = user;
    }

    public static UserPoints of(User userRef) {
        return new UserPoints(userRef);
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    public void subtractPoints(int amount) {
        this.points = Math.max(0, this.points - amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPoints other)) return false;
        return Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
