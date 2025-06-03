package com.eventitta.event.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 데이터 출처 예: "SEOUL", "NATIONAL" 등
     */
    @Column(length = 50, nullable = false)
    private String source;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String place;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String category;

    @Column(name = "is_free", nullable = false)
    private boolean isFree = false;

    @Column(length = 100)
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

    @Column(name = "created_at", nullable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    protected Event() {
    }

    private Event(String source,
                  String title,
                  String description,
                  String place,
                  String address,
                  String category,
                  boolean isFree,
                  String useFee,
                  String homepageUrl,
                  String mainImgUrl,
                  BigDecimal latitude,
                  BigDecimal longitude,
                  LocalDateTime startTime,
                  LocalDateTime endTime) {
        this.source = source;
        this.title = title;
        this.description = description;
        this.place = place;
        this.address = address;
        this.category = category;
        this.isFree = isFree;
        this.useFee = useFee;
        this.homepageUrl = homepageUrl;
        this.mainImgUrl = mainImgUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static EventBuilder builder() {
        return new EventBuilder();
    }

    public static class EventBuilder {
        private String source;
        private String title;
        private String description;
        private String place;
        private String address;
        private String category;
        private boolean isFree;
        private String useFee;
        private String homepageUrl;
        private String mainImgUrl;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        EventBuilder() {
        }

        public EventBuilder source(String source) {
            this.source = source;
            return this;
        }

        public EventBuilder title(String title) {
            this.title = title;
            return this;
        }

        public EventBuilder description(String description) {
            this.description = description;
            return this;
        }

        public EventBuilder place(String place) {
            this.place = place;
            return this;
        }

        public EventBuilder address(String address) {
            this.address = address;
            return this;
        }

        public EventBuilder category(String category) {
            this.category = category;
            return this;
        }

        public EventBuilder isFree(boolean isFree) {
            this.isFree = isFree;
            return this;
        }

        public EventBuilder useFee(String useFee) {
            this.useFee = useFee;
            return this;
        }

        public EventBuilder homepageUrl(String homepageUrl) {
            this.homepageUrl = homepageUrl;
            return this;
        }

        public EventBuilder mainImgUrl(String mainImgUrl) {
            this.mainImgUrl = mainImgUrl;
            return this;
        }

        public EventBuilder latitude(BigDecimal latitude) {
            this.latitude = latitude;
            return this;
        }

        public EventBuilder longitude(BigDecimal longitude) {
            this.longitude = longitude;
            return this;
        }

        public EventBuilder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public EventBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Event build() {
            return new Event(
                source, title, description, place, address, category,
                isFree, useFee, homepageUrl, mainImgUrl,
                latitude, longitude, startTime, endTime
            );
        }
    }
}
