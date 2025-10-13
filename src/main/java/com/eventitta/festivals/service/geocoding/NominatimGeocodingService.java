package com.eventitta.festivals.service.geocoding;

import com.eventitta.festivals.service.geocoding.dto.Coordinates;
import com.eventitta.festivals.service.geocoding.dto.NominatimResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class NominatimGeocodingService implements GeocodingService {

    private final GeocodingProperties properties;
    private final RestClient geocodingRestClient;

    public NominatimGeocodingService(
        GeocodingProperties properties,
        @Qualifier("geocodingRestClient") RestClient geocodingRestClient) {
        this.properties = properties;
        this.geocodingRestClient = geocodingRestClient;
    }

    @Override
    public Optional<Coordinates> getCoordinates(String address) {
        if (!properties.isEnabled()) {
            log.debug("지오코딩이 비활성화되어 있습니다.");
            return Optional.empty();
        }

        if (address == null || address.trim().isEmpty()) {
            log.warn("주소가 비어있습니다.");
            return Optional.empty();
        }

        String cleanedAddress = preprocessAddress(address);

        try {
            Thread.sleep(properties.getRequestDelayMs());

            List<NominatimResponse> responses = geocodingRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/search")
                    .queryParam("q", cleanedAddress)
                    .queryParam("format", "json")
                    .queryParam("accept-language", "ko")
                    .queryParam("countrycodes", "kr")
                    .queryParam("limit", "1")
                    .build()
                )
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

            if (responses == null || responses.isEmpty()) {
                log.warn("주소 '{}'에 대한 지오코딩 결과가 없습니다.", cleanedAddress);
                return Optional.empty();
            }

            NominatimResponse response = responses.get(0);
            BigDecimal latitude = toDecimal(response.lat());
            BigDecimal longitude = toDecimal(response.lon());

            if (latitude == null || longitude == null) {
                log.warn("지오코딩 응답의 좌표가 유효하지 않습니다. address: {}", address);
                return Optional.empty();
            }

            log.info("지오코딩 성공: {} -> ({}, {})", address, latitude, longitude);
            return Optional.of(new Coordinates(latitude, longitude));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("지오코딩 요청 중 인터럽트 발생: {}", address, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("지오코딩 API 호출 실패: {}", address, e);
            return Optional.empty();
        }
    }

    private BigDecimal toDecimal(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String preprocessAddress(String address) {
        if (address == null) {
            return "";
        }

        String cleaned = address.trim();
        cleaned = cleaned.replaceAll("\\s*지하\\s*", " ");
        cleaned = cleaned.replaceAll("\\s*[A-Z]?\\d+[층F]?\\s*", " ");
        cleaned = cleaned.replaceAll("\\s*[가-힣]+[홀관실장센터룸]\\s*$", "");
        cleaned = cleaned.replaceAll("\\([^)]*\\)", "");
        cleaned = cleaned.replaceAll("[,./~·]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("\\s+", "");
        log.debug("주소 전처리: '{}' -> '{}'", address, cleaned);

        return cleaned;
    }
}
