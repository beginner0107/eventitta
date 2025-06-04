package com.eventitta.event.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String place;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String category;

    @Column(name = "is_free", nullable = false)
    private Boolean isFree;

    @Column(length = 255)
    private String useFee;

    @Column(name = "homepage_url", length = 512)
    private String homepageUrl;

    @Column(name = "main_img_url", length = 512)
    private String mainImgUrl;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * null이 아닌 필드만 업데이트.
     */
    public void updateFrom(Event other) {
        if (other.getDescription() != null && !other.getDescription().equals(this.description)) {
            this.description = other.getDescription();
        }
        if (other.getPlace() != null && !other.getPlace().equals(this.place)) {
            this.place = other.getPlace();
        }
        if (other.getAddress() != null && !other.getAddress().equals(this.address)) {
            this.address = other.getAddress();
        }
        if (other.getCategory() != null && !other.getCategory().equals(this.category)) {
            this.category = other.getCategory();
        }
        if (other.getIsFree() != null && !other.getIsFree().equals(this.isFree)) {
            this.isFree = other.getIsFree();
        }
        if (other.getUseFee() != null && !other.getUseFee().equals(this.useFee)) {
            this.useFee = other.getUseFee();
        }
        if (other.getHomepageUrl() != null && !other.getHomepageUrl().equals(this.homepageUrl)) {
            this.homepageUrl = other.getHomepageUrl();
        }
        if (other.getMainImgUrl() != null && !other.getMainImgUrl().equals(this.mainImgUrl)) {
            this.mainImgUrl = other.getMainImgUrl();
        }
        if (other.getLatitude() != null && !other.getLatitude().equals(this.latitude)) {
            this.latitude = other.getLatitude();
        }
        if (other.getLongitude() != null && !other.getLongitude().equals(this.longitude)) {
            this.longitude = other.getLongitude();
        }
        if (other.getStartTime() != null && !other.getStartTime().equals(this.startTime)) {
            this.startTime = other.getStartTime();
        }
        if (other.getEndTime() != null && !other.getEndTime().equals(this.endTime)) {
            this.endTime = other.getEndTime();
        }
    }
}
