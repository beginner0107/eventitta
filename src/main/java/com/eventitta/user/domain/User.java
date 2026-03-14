package com.eventitta.user.domain;

import com.eventitta.common.domain.BaseEntity;
import com.eventitta.user.exception.UserErrorCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")
    }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String email;

    @Column
    private String password;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String nickname;

    @Column(name = "profile_picture_url", length = 512)
    private String profilePictureUrl;

    @Column(name = "self_intro", columnDefinition = "TEXT")
    private String selfIntro;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interests")
    private List<String> interests;

    @Size(max = 255)
    private String address;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false)
    private int points = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Provider provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    public void updateProfile(
        String nickname,
        String profilePictureUrl,
        String selfIntro,
        List<String> interests,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
    ) {
        this.nickname = nickname;
        this.profilePictureUrl = profilePictureUrl;
        this.selfIntro = selfIntro;
        this.interests = interests;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void delete() {
        String token = generateDeletedToken();
        this.nickname = token;
        this.email = token + "@deleted.local";
        this.password = null;
        this.profilePictureUrl = null;
        this.selfIntro = null;
        this.interests = null;
        this.address = null;
        this.latitude = null;
        this.longitude = null;
        this.deleted = true;
    }

    private String generateDeletedToken() {
        return "__deleted_user_" + this.id + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    public void earnPoints(int amount) {
        if (amount <= 0) {
            throw UserErrorCode.INVALID_POINTS_AMOUNT.defaultException();
        }
        this.points += amount;
    }

    public void deductPoints(int amount) {
        if (amount <= 0) {
            throw UserErrorCode.INVALID_POINTS_AMOUNT.defaultException();
        }

        if (this.points < amount) {
            throw UserErrorCode.INSUFFICIENT_POINTS.defaultException();
        }

        this.points -= amount;
    }
}
