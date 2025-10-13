package com.eventitta.festivals.service.geocoding;

import com.eventitta.festivals.service.geocoding.dto.Coordinates;

import java.util.Optional;


public interface GeocodingService {
    /**
     * 주소를 위경도 좌표로 변환
     *
     * @param address 변환할 주소
     * @return 좌표 (실패 시 Optional.empty())
     */
    Optional<Coordinates> getCoordinates(String address);
}
