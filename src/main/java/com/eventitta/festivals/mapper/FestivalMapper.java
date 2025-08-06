package com.eventitta.festivals.mapper;

import com.eventitta.festivals.domain.Festival;
import com.eventitta.festivals.domain.DataSource;
import com.eventitta.festivals.dto.NationalFestivalItem;
import com.eventitta.festivals.dto.SeoulFestivalRow;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Component
public class FestivalMapper {

    public Festival from(SeoulFestivalRow row) {
        Festival event = new Festival();

        event.setTitle(validateTitle(row.getTITLE()));
        event.setVenue(row.getPLACE());
        event.setStartDate(parseLocalDate(row.getSTRTDATE()));
        event.setEndDate(parseLocalDate(row.getEND_DATE()));
        event.setCategory(row.getCODENAME());
        event.setDistrict(row.getGUNAME());
        event.setTargetAudience(row.getUSE_TRGT());
        event.setFeeInfo(row.getUSE_FEE());
        event.setIsFree("무료".equals(row.getIS_FREE()));
        event.setPerformers(row.getPLAYER());
        event.setProgramInfo(row.getPROGRAM());
        event.setMainImageUrl(row.getMAIN_IMG());
        event.setThemeCode(row.getTHEMECODE());
        event.setTicketType(row.getTICKET());
        event.setOrganizer(row.getORG_NAME());
        event.setHomepageUrl(row.getHMPG_ADDR());
        event.setDetailUrl(row.getORG_LINK());
        event.setLatitude(toDecimal(row.getLAT()));
        event.setLongitude(toDecimal(row.getLOT()));
        event.setContent(row.getETC_DESC());
        event.setDataSource(DataSource.SEOUL_FESTIVAL);
        event.setExternalId(generateExternalId(row));
        event.setContentHash(generateContentHash(row));

        return event;
    }

    public Festival from(NationalFestivalItem item) {
        Festival event = new Festival();

        event.setTitle(validateTitle(item.getFstvlNm()));
        event.setVenue(item.getOpar());
        event.setStartDate(parseNationalDate(item.getFstvlStartDate()));
        event.setEndDate(parseNationalDate(item.getFstvlEndDate()));
        event.setContent(item.getFstvlCo());
        event.setOrganizer(item.getMnnstNm());
        event.setHomepageUrl(item.getHomepageUrl());
        event.setLatitude(toDecimal(item.getLatitude()));
        event.setLongitude(toDecimal(item.getLongitude()));
        event.setDataSource(DataSource.NATIONAL_FESTIVAL);
        event.setExternalId(generateNationalExternalId(item));
        event.setContentHash(generateNationalContentHash(item));

        return event;
    }

    private LocalDate parseLocalDate(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty() || dateTime.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(dateTime.substring(0, 10)); // "yyyy-MM-dd"
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseNationalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            // 국가 축제 API는 "yyyy-MM-dd" 형식으로 제공
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toDecimal(String val) {
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return null;
        }
    }

    private String generateExternalId(SeoulFestivalRow row) {
        String title = validateTitle(row.getTITLE());
        String place = row.getPLACE() != null ? row.getPLACE() : "";
        String startDate = row.getSTRTDATE() != null ? row.getSTRTDATE() : "";
        String category = row.getCODENAME() != null ? row.getCODENAME() : "";
        String organizer = row.getORG_NAME() != null ? row.getORG_NAME() : "";
        String themeCode = row.getTHEMECODE() != null ? row.getTHEMECODE() : "";

        // 더 많은 필드를 포함하여 고유성 확보
        String base = title + place + startDate + category + organizer + themeCode;
        return DigestUtils.md5DigestAsHex(base.getBytes(StandardCharsets.UTF_8));
    }

    private String generateContentHash(SeoulFestivalRow row) {
        String title = validateTitle(row.getTITLE());
        String venue = row.getPLACE() != null ? row.getPLACE() : "";
        LocalDate start = parseLocalDate(row.getSTRTDATE());
        LocalDate end = parseLocalDate(row.getEND_DATE());
        String startDate = start != null ? start.toString() : "";
        String endDate = end != null ? end.toString() : "";
        String base = title + startDate + endDate + venue;
        return DigestUtils.md5DigestAsHex(base.getBytes(StandardCharsets.UTF_8));
    }

    private String generateNationalExternalId(NationalFestivalItem item) {
        String title = validateTitle(item.getFstvlNm());
        String place = item.getOpar() != null ? item.getOpar() : "";
        String startDate = item.getFstvlStartDate() != null ? item.getFstvlStartDate() : "";
        String organizer = item.getMnnstNm() != null ? item.getMnnstNm() : "";

        String base = title + place + startDate + organizer;
        return DigestUtils.md5DigestAsHex(base.getBytes(StandardCharsets.UTF_8));
    }

    private String generateNationalContentHash(NationalFestivalItem item) {
        String title = validateTitle(item.getFstvlNm());
        String venue = item.getOpar() != null ? item.getOpar() : "";
        LocalDate start = parseNationalDate(item.getFstvlStartDate());
        LocalDate end = parseNationalDate(item.getFstvlEndDate());
        String startDate = start != null ? start.toString() : "";
        String endDate = end != null ? end.toString() : "";
        String base = title + startDate + endDate + venue;
        return DigestUtils.md5DigestAsHex(base.getBytes(StandardCharsets.UTF_8));
    }

    private String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "제목 없음"; // 기본값 설정
        }
        return title.trim();
    }
}
