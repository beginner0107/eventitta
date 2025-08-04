package com.eventitta.festival.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cultural_events",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"external_id", "data_source"})
    },
    indexes = {
        @Index(name = "idx_location", columnList = "latitude, longitude"),
        @Index(name = "idx_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_data_source", columnList = "data_source")
    })
@Getter
@Setter
@NoArgsConstructor
public class CulturalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String title; // 행사/축제명

    @Column(name = "venue", length = 1000)
    private String venue; // 개최장소

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // 서울시 API 전용 필드들
    @Column(name = "category", length = 100)
    private String category; // 분류 (CODENAME)

    @Column(name = "district", length = 100)
    private String district; // 자치구 (GUNAME)

    @Column(name = "target_audience", length = 500)
    private String targetAudience; // 이용대상

    @Column(name = "fee_info", length = 1000)
    private String feeInfo; // 이용요금

    @Column(name = "is_free")
    private Boolean isFree; // 무료 여부

    @Column(name = "performers", columnDefinition = "TEXT")
    private String performers; // 출연자정보

    @Column(name = "program_info", columnDefinition = "TEXT")
    private String programInfo; // 프로그램소개

    @Column(name = "main_image_url", length = 1000)
    private String mainImageUrl; // 대표이미지

    @Column(name = "theme_code", length = 100)
    private String themeCode; // 테마분류

    @Column(name = "ticket_type", length = 50)
    private String ticketType; // 시민/기관

    // 공통 필드들
    @Column(name = "organizer", length = 500)
    private String organizer; // 주관기관/기관명

    @Column(name = "host", length = 500)
    private String host; // 주최기관 (전국축제만)

    @Column(name = "supporter", length = 500)
    private String supporter; // 후원기관 (전국축제만)

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "homepage_url", length = 1000)
    private String homepageUrl;

    @Column(name = "detail_url", length = 1000)
    private String detailUrl; // 상세페이지 URL

    @Column(name = "road_address", length = 500)
    private String roadAddress;

    @Column(name = "jibun_address", length = 500)
    private String jibunAddress;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // 행사내용/기타내용

    @Column(name = "related_info", columnDefinition = "TEXT")
    private String relatedInfo; // 관련정보 (전국축제만)

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false)
    private DataSource dataSource;

    // 중복 방지용 필드들
    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(name = "content_hash", length = 32)
    private String contentHash;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void updateFrom(CulturalEvent src) {
        this.title = src.getTitle();
        this.venue = src.getVenue();
        this.startDate = src.getStartDate();
        this.endDate = src.getEndDate();
        this.category = src.getCategory();
        this.district = src.getDistrict();
        this.targetAudience = src.getTargetAudience();
        this.feeInfo = src.getFeeInfo();
        this.isFree = src.getIsFree();
        this.performers = src.getPerformers();
        this.programInfo = src.getProgramInfo();
        this.mainImageUrl = src.getMainImageUrl();
        this.themeCode = src.getThemeCode();
        this.ticketType = src.getTicketType();
        this.organizer = src.getOrganizer();
        this.homepageUrl = src.getHomepageUrl();
        this.detailUrl = src.getDetailUrl();
        this.latitude = src.getLatitude();
        this.longitude = src.getLongitude();
        this.content = src.getContent();
        this.contentHash = src.getContentHash();
    }
}

