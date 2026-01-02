package com.eventitta.festivals.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 축제 데이터 동기화 API 응답
 * Admin API에서 수동 동기화 작업의 결과를 반환할 때 사용
 */
@Schema(description = "축제 데이터 동기화 응답")
public record FestivalSyncResponse(
    @Schema(description = "처리 상태", example = "success")
    String status,

    @Schema(description = "응답 메시지", example = "전국 축제 데이터 동기화가 완료되었습니다.")
    String message
) {
    /**
     * 성공 응답 생성
     *
     * @param message 성공 메시지
     * @return 성공 상태의 FestivalSyncResponse
     */
    public static FestivalSyncResponse success(String message) {
        return new FestivalSyncResponse("success", message);
    }
}
