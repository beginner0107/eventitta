package com.eventitta.festivals.mapper;

import com.eventitta.festivals.domain.Festival;
import com.eventitta.festivals.dto.NationalFestivalItem;
import com.eventitta.festivals.dto.SeoulFestivalRow;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface FestivalMapper {

    default Festival from(SeoulFestivalRow row) {
        return Festival.createSeoulFestival(
            validateTitle(row.title()),
            row.place(),
            parseSeoulDate(row.startDate()),
            parseSeoulDate(row.endDate()),
            row.codeName(),
            row.guName(),
            row.useTarget(),
            row.useFee(),
            mapIsFree(row.isFree()),
            row.player(),
            row.program(),
            row.mainImg(),
            row.themeCode(),
            row.ticket(),
            row.orgName(),
            row.homepageAddr(),
            row.orgLink(),
            toDecimal(row.latitude()),
            toDecimal(row.longitude()),
            row.etcDesc(),
            generateSeoulExternalId(row)
        );
    }

    default Festival from(NationalFestivalItem item) {
        return Festival.createNationalFestival(
            validateTitle(item.fstvlNm()),
            item.opar(),
            parseNationalDate(item.fstvlStartDate()),
            parseNationalDate(item.fstvlEndDate()),
            item.fstvlCo(),
            item.mnnstNm(),
            item.homepageUrl(),
            toDecimal(item.latitude()),
            toDecimal(item.longitude()),
            generateNationalExternalId(item)
        );
    }

    @Named("validateTitle")
    default String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "제목 없음";
        }
        return title.trim();
    }

    @Named("parseSeoulDate")
    default LocalDate parseSeoulDate(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty() || dateTime.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(dateTime.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    @Named("parseNationalDate")
    default LocalDate parseNationalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Named("toDecimal")
    default BigDecimal toDecimal(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Named("mapIsFree")
    default Boolean mapIsFree(String isFree) {
        return "무료".equals(isFree);
    }

    // 서울시 축제용 고유 식별자 생성 (기존 로직 유지)
    @Named("generateSeoulExternalId")
    default String generateSeoulExternalId(SeoulFestivalRow row) {
        String title = validateTitle(row.title());
        String place = row.place() != null ? row.place() : "";
        String startDate = row.startDate() != null ? row.startDate() : "";
        String category = row.codeName() != null ? row.codeName() : "";
        String organizer = row.orgName() != null ? row.orgName() : "";
        String themeCode = row.themeCode() != null ? row.themeCode() : "";

        String base = title + place + startDate + category + organizer + themeCode;
        return DigestUtils.md5DigestAsHex(base.getBytes(StandardCharsets.UTF_8));
    }

    // 전국축제용 고유 식별자 생성 (기존 로직 유지)
    @Named("generateNationalExternalId")
    default String generateNationalExternalId(NationalFestivalItem item) {
        String title = validateTitle(item.fstvlNm());
        String place = item.opar() != null ? item.opar() : "";
        String startDate = item.fstvlStartDate() != null ? item.fstvlStartDate() : "";
        String organizer = item.mnnstNm() != null ? item.mnnstNm() : "";

        String base = title + place + startDate + organizer;
        return DigestUtils.md5DigestAsHex(base.getBytes(StandardCharsets.UTF_8));
    }
}
