package com.eventitta.event.mapper;

import com.eventitta.event.domain.Event;
import com.eventitta.event.dto.FestivalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class FestivalMapper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 단일 FestivalItem을 Event 엔티티로 변환합니다.
     *
     * @param item   API로부터 받은 축제 정보 DTO
     * @param source 데이터 출처(ex: "SEOUL")
     * @return Event 엔티티 인스턴스
     */
    public Event toEntity(FestivalApiResponse.FestivalItem item, String source) {
        LocalDate startLocalDate = LocalDate.parse(item.getStartDate(), DATE_FMT);
        LocalDate endLocalDate = LocalDate.parse(item.getEndDate(), DATE_FMT);

        LocalDateTime startDateTime = startLocalDate.atStartOfDay();
        LocalDateTime endDateTime = endLocalDate.atTime(23, 59, 59);

        BigDecimal lat = null;
        BigDecimal lng = null;
        try {
            if (item.getLatitude() != null && !item.getLatitude().isBlank()) {
                lat = new BigDecimal(item.getLatitude());
            }
            if (item.getLongitude() != null && !item.getLongitude().isBlank()) {
                lng = new BigDecimal(item.getLongitude());
            }
        } catch (NumberFormatException e) {
            log.error("[FestivalMapper] 위도/경도 파싱 오류: {}", e.getMessage());
        }

        String address = item.getRoadAddress();
        if (address == null || address.isBlank()) {
            address = item.getLandAddress();
        }

        boolean isFree = true;
        String useFee = null; // 별도 요금 정보가 없으므로 null

        String mainImgUrl = null;

        String category = "축제";

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
