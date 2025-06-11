package com.eventitta.event.mapper;

import com.eventitta.event.domain.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import static com.eventitta.event.dto.response.SeoulFestivalResponse.SeoulEventItem;

@Slf4j
@Component
public class SeoulFestivalMapper implements FestivalToEventMapper<SeoulEventItem> {

    private static final DateTimeFormatter DATE_TIME_FMT = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .optionalStart()
        .appendPattern(".S")
        .optionalEnd()
        .toFormatter();

    @Override
    public Event toEntity(SeoulEventItem dto, String source) {
        // (1) 시작/종료일 파싱
        LocalDateTime start = null, end = null;
        if (dto.getStartDate() != null && !dto.getStartDate().isBlank()) {
            try {
                start = LocalDateTime.parse(dto.getStartDate(), DATE_TIME_FMT);
            } catch (DateTimeParseException e) {
                log.warn("SeoulMapper: startDate 파싱 실패 → {}", dto.getStartDate());
            }
        }
        if (dto.getEndDate() != null && !dto.getEndDate().isBlank()) {
            try {
                end = LocalDateTime.parse(dto.getEndDate(), DATE_TIME_FMT);
            } catch (DateTimeParseException e) {
                log.warn("SeoulMapper: endDate 파싱 실패 → {}", dto.getEndDate());
            }
        }

        // (2) 위도/경도 파싱
        BigDecimal latitude = null, longitude = null;
        try {
            if (dto.getLat() != null && !dto.getLat().isBlank()) {
                latitude = new BigDecimal(dto.getLat());
            }
            if (dto.getLot() != null && !dto.getLot().isBlank()) {
                longitude = new BigDecimal(dto.getLot());
            }
        } catch (NumberFormatException e) {
            log.error("SeoulMapper: 위도/경도 파싱 실패 → {}", e.getMessage());
        }

        // (3) isFree (“무료”/“유료”) → Boolean
        Boolean isFree = null;
        if ("무료".equals(dto.getIsFree())) {
            isFree = true;
        } else if ("유료".equals(dto.getIsFree())) {
            isFree = false;
        }

        return Event.builder()
            .source(source)
            .title(dto.getTitle())
            .description(dto.getEtcDesc())
            .place(dto.getPlace())
            .address(dto.getPlace())
            .category(dto.getCategory())
            .isFree(isFree)
            .useFee(dto.getUseFee())
            .homepageUrl(dto.getHomepageUrl())
            .mainImgUrl(dto.getMainImgUrl())
            .latitude(latitude)
            .longitude(longitude)
            .startTime(start)
            .endTime(end)
            .build();
    }
}
