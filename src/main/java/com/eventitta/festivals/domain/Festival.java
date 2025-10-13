package com.eventitta.festivals.domain;

import com.eventitta.common.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.eventitta.common.config.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "festivals",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"external_id", "data_source"})
    },
    indexes = {
        @Index(name = "idx_location", columnList = "latitude, longitude"),
        @Index(name = "idx_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_data_source", columnList = "data_source")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Festival extends BaseTimeEntity {

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

    @Column(name = "homepage_url", length = 1000)
    private String homepageUrl;

    @Column(name = "detail_url", length = 1000)
    private String detailUrl; // 상세페이지 URL

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // 행사내용/기타내용

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false)
    private DataSource dataSource;

    // 중복 방지용 필드
    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Builder(access = AccessLevel.PRIVATE)
    private Festival(String title, String venue, LocalDate startDate, LocalDate endDate,
                     String category, String district, String targetAudience, String feeInfo,
                     Boolean isFree, String performers, String programInfo, String mainImageUrl,
                     String themeCode, String ticketType, String organizer, String homepageUrl, String detailUrl,
                     BigDecimal latitude, BigDecimal longitude,
                     String content, DataSource dataSource, String externalId) {
        this.title = title;
        this.venue = venue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.category = category;
        this.district = district;
        this.targetAudience = targetAudience;
        this.feeInfo = feeInfo;
        this.isFree = isFree;
        this.performers = performers;
        this.programInfo = programInfo;
        this.mainImageUrl = mainImageUrl;
        this.themeCode = themeCode;
        this.ticketType = ticketType;
        this.organizer = organizer;
        this.homepageUrl = homepageUrl;
        this.detailUrl = detailUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.content = content;
        this.dataSource = dataSource;
        this.externalId = externalId;
    }

    public static Festival createSeoulFestival(String title, String venue, LocalDate startDate, LocalDate endDate,
                                               String category, String district, String targetAudience, String feeInfo,
                                               Boolean isFree, String performers, String programInfo, String mainImageUrl,
                                               String themeCode, String ticketType, String organizer, String homepageUrl,
                                               String detailUrl, BigDecimal latitude, BigDecimal longitude,
                                               String content, String externalId) {
        return Festival.builder()
            .title(title)
            .venue(venue)
            .startDate(startDate)
            .endDate(endDate)
            .category(category)
            .district(district)
            .targetAudience(targetAudience)
            .feeInfo(feeInfo)
            .isFree(isFree)
            .performers(performers)
            .programInfo(programInfo)
            .mainImageUrl(mainImageUrl)
            .themeCode(themeCode)
            .ticketType(ticketType)
            .organizer(organizer)
            .homepageUrl(homepageUrl)
            .detailUrl(detailUrl)
            .latitude(latitude)
            .longitude(longitude)
            .content(content)
            .dataSource(DataSource.SEOUL_FESTIVAL)
            .externalId(externalId)
            .build();
    }

    public static Festival createNationalFestival(String title, String venue, LocalDate startDate, LocalDate endDate,
                                                  String content, String organizer, String homepageUrl,
                                                  BigDecimal latitude, BigDecimal longitude, String externalId) {
        return Festival.builder()
            .title(title)
            .venue(venue)
            .startDate(startDate)
            .endDate(endDate)
            .content(content)
            .organizer(organizer)
            .homepageUrl(homepageUrl)
            .latitude(latitude)
            .longitude(longitude)
            .dataSource(DataSource.NATIONAL_FESTIVAL)
            .externalId(externalId)
            .build();
    }

    public void updateFestivalInfo(Festival updatedFestival) {
        this.title = updatedFestival.getTitle();
        this.venue = updatedFestival.getVenue();
        this.startDate = updatedFestival.getStartDate();
        this.endDate = updatedFestival.getEndDate();
        this.category = updatedFestival.getCategory();
        this.district = updatedFestival.getDistrict();
        this.targetAudience = updatedFestival.getTargetAudience();
        this.feeInfo = updatedFestival.getFeeInfo();
        this.isFree = updatedFestival.getIsFree();
        this.performers = updatedFestival.getPerformers();
        this.programInfo = updatedFestival.getProgramInfo();
        this.mainImageUrl = updatedFestival.getMainImageUrl();
        this.themeCode = updatedFestival.getThemeCode();
        this.ticketType = updatedFestival.getTicketType();
        this.organizer = updatedFestival.getOrganizer();
        this.homepageUrl = updatedFestival.getHomepageUrl();
        this.detailUrl = updatedFestival.getDetailUrl();
        if (updatedFestival.getLatitude() != null && updatedFestival.getLongitude() != null) {
            this.latitude = updatedFestival.getLatitude();
            this.longitude = updatedFestival.getLongitude();
        }
        this.content = updatedFestival.getContent();
    }

    public void updateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
