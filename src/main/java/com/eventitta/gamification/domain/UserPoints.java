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
    private Long userId; // @MapsId로 user의 PK를 공유. 직접 세팅 금지!

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private int points = 0;

    // 외부에서 실수로 잘못 채우지 못하게 private 생성자 + 팩토리 메서드
    private UserPoints(User user, int point) {
        this.user = user;  // userId는 @MapsId가 자동 동기화
        this.points = point;
    }

    public static UserPoints of(User userRef, int point) {
        return new UserPoints(userRef, point);
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    public void subtractPoints(int amount) {
        this.points = Math.max(0, this.points - amount);
    }

    // 동등성은 식별자 기반으로만 (영속성 컨텍스트 안전)
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
