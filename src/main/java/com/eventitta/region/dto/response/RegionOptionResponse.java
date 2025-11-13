package com.eventitta.region.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "셀렉트 박스용 지역 옵션 DTO")
public record RegionOptionResponse(
    @Schema(description = "지역 코드 (최하위)", example = "1100110100")
    String code,

    @Schema(description = "전체 경로 표시용 이름", example = "서울특별시 > 종로구 > 청운효자동")
    String displayName,

    @Schema(description = "계층 코드 배열 (상위 → 하위)", example = "[\"1100000000\", \"1100100000\", \"1100110100\"]")
    List<String> pathCodes,

    @Schema(description = "계층 이름 배열 (상위 → 하위)", example = "[\"서울특별시\", \"종로구\", \"청운효자동\"]")
    List<String> pathNames
) {
    /**
     * 하위 호환성을 위한 fullCode 생성
     *
     * @return 하이픈으로 연결된 전체 코드 (예: "1100000000-1100100000-1100110100")
     * @deprecated 대신 {@link #pathCodes()}를 사용하세요
     */
    @Deprecated(since = "1.1.0")
    public String fullCode() {
        return String.join("-", pathCodes);
    }
}
