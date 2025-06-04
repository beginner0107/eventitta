package com.eventitta.event.client;

import com.eventitta.common.config.RestClientConfig.SeoulApi;
import com.eventitta.event.dto.SeoulApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeoulApiClient {

    private final SeoulApi seoulApi;

    @Value("${festival.seoul.service-key}")
    private String serviceKey;

    @Value("${festival.seoul.file-type}")
    private String fileType;

    @Value("${festival.seoul.service-name}")
    private String serviceName;

    public SeoulApiClient(SeoulApi seoulApi) {
        this.seoulApi = seoulApi;
    }

    /**
     * startIndex부터 endIndex까지 범위의 데이터를 요청.
     * 마지막 파라미터 dateParam은 “조회 기준일”이나 “검색조건” 등이 필요한 경우 사용.
     * 예) "20230101" 처럼 YYYYMMDD 형태 문자열.
     */
    public SeoulApiResponse.SeoulResponseWrapper fetchSeoulPage(
        int startIndex, int endIndex, String unused1, String unused2, String dateParam) {
        log.debug("SeoulApiClient 호출 - serviceKey={}, fileType={}, serviceName={}, start={}, end={}, date={}",
            serviceKey, fileType, serviceName, startIndex, endIndex, dateParam);

        SeoulApiResponse response = seoulApi.getSeoulEvents(
            serviceKey,
            fileType,
            serviceName,
            startIndex,
            endIndex,
            unused1,
            unused2,
            dateParam
        );

        if (response == null || response.getResponseWrapper() == null) {
            throw new RuntimeException("서울시 API 호출 결과가 null");
        }
        return response.getResponseWrapper();
    }
}
