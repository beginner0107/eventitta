package com.eventitta.event.mapper;

import com.eventitta.event.domain.Event;

import static com.eventitta.event.dto.NationalFestivalResponse.FestivalItem;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class NationalFestivalMapper implements FestivalToEventMapper<FestivalItem> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Event toEntity(FestivalItem item, String source) {
        // (1) 날짜 파싱
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        try {
            LocalDate startLocal = LocalDate.parse(item.getStartDate(), DATE_FMT);
            startDateTime = startLocal.atStartOfDay();
        } catch (DateTimeParseException e) {
            log.warn("NationalMapper: startDate 파싱 실패 → {}", item.getStartDate());
        }
        try {
            LocalDate endLocal = LocalDate.parse(item.getEndDate(), DATE_FMT);
            endDateTime = endLocal.atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            log.warn("NationalMapper: endDate 파싱 실패 → {}", item.getEndDate());
        }

        // (2) 위도/경도 파싱
        BigDecimal lat = null, lng = null;
        try {
            if (item.getLatitude() != null && !item.getLatitude().isBlank()) {
                lat = new BigDecimal(item.getLatitude());
            }
            if (item.getLongitude() != null && !item.getLongitude().isBlank()) {
                lng = new BigDecimal(item.getLongitude());
            }
        } catch (NumberFormatException e) {
            log.error("NationalMapper: 위도/경도 파싱 실패 → {}", e.getMessage());
        }

        // (3) 주소 (도로명 우선, 없으면 지번)
        String address = item.getRoadAddress();
        if (address == null || address.isBlank()) {
            address = item.getLandAddress();
        }

        // (4) 기본값들
        boolean isFree = true;  // 무료/유료 정보가 없으므로 기본 true
        String useFee = null;   // 별도 요금 정보 없음
        String mainImgUrl = null;
        String category = "축제";

        // (5) Event 빌드
        return Event.builder()
            .source(source)
            .title(item.getFestivalName())
            .description(item.getDescription())
            .place(item.getPlace())
            .address(address)
            .category(category)
            .isFree(isFree)
            .useFee(useFee)
            .homepageUrl(item.getHomepageUrl())
            .mainImgUrl(mainImgUrl)
            .latitude(lat)
            .longitude(lng)
            .startTime(startDateTime)
            .endTime(endDateTime)
            .build();
    }
}
