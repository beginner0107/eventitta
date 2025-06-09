package com.eventitta.event.domain;

import com.eventitta.common.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Entity
@Table(
    name = "events",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_events_source_title_start_time",
            columnNames = {"source", "title", "start_time"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String place;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String category;

    @Column(name = "is_free", nullable = true)
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

    public void updateFrom(Event other) {
        if (other.getDescription() != null &&
            !Objects.equals(other.getDescription(), this.description)) {
            this.description = other.getDescription();
        }

        if (other.getPlace() != null &&
            !Objects.equals(other.getPlace(), this.place)) {
            this.place = other.getPlace();
        }

        if (other.getAddress() != null &&
            !Objects.equals(other.getAddress(), this.address)) {
            this.address = other.getAddress();
        }

        if (other.getCategory() != null &&
            !Objects.equals(other.getCategory(), this.category)) {
            this.category = other.getCategory();
        }

        if (other.getIsFree() != null &&
            !Objects.equals(other.getIsFree(), this.isFree)) {
            this.isFree = other.getIsFree();
        }

        if (other.getUseFee() != null &&
            !Objects.equals(other.getUseFee(), this.useFee)) {
            this.useFee = other.getUseFee();
        }

        if (other.getHomepageUrl() != null &&
            !Objects.equals(other.getHomepageUrl(), this.homepageUrl)) {
            this.homepageUrl = other.getHomepageUrl();
        }

        if (other.getMainImgUrl() != null &&
            !Objects.equals(other.getMainImgUrl(), this.mainImgUrl)) {
            this.mainImgUrl = other.getMainImgUrl();
        }

        if (other.getLatitude() != null &&
            !Objects.equals(other.getLatitude(), this.latitude)) {
            this.latitude = other.getLatitude();
        }

        if (other.getLongitude() != null &&
            !Objects.equals(other.getLongitude(), this.longitude)) {
            this.longitude = other.getLongitude();
        }

        if (other.getStartTime() != null &&
            !Objects.equals(other.getStartTime(), this.startTime)) {
            this.startTime = other.getStartTime();
        }

        if (other.getEndTime() != null &&
            !Objects.equals(other.getEndTime(), this.endTime)) {
            this.endTime = other.getEndTime();
        }
    }
}
