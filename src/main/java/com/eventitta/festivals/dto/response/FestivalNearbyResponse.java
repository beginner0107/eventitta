package com.eventitta.festivals.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FestivalNearbyResponse {
    private Long id;
    private String title;
    private String place;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDateTime endTime;

    private String category;
    private Boolean isFree;
    private String homepageUrl;
    private Double distance;

    // JPA 네이티브 쿼리 결과 매핑을 위한 생성자
    public FestivalNearbyResponse(Long id, String title, String place, LocalDateTime startTime,
                               LocalDateTime endTime, String category, Boolean isFree,
                               String homepageUrl, Double distance) {
        this.id = id;
        this.title = title;
        this.place = place;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.isFree = isFree;
        this.homepageUrl = homepageUrl;
        this.distance = distance;
    }
}
