package com.eventitta.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API 에러 응답")
public record ApiErrorResponse(
    @Schema(description = "에러 코드", example = "INVALID_INPUT")
    String error,

    @Schema(description = "에러 메시지", example = "email: 올바른 이메일 형식이 아닙니다.")
    String message,

    @Schema(description = "HTTP 상태 코드", example = "400")
    int status,

    @Schema(description = "요청 경로", example = "/api/v1/users")
    String path,

    @Schema(description = "에러 발생 시각 (ISO-8601)", example = "2025-05-07T13:30:20Z")
    String timestamp
) {
    public static ApiErrorResponse of(String error, String message, int status, String path) {
        return new ApiErrorResponse(
            error,
            message,
            status,
            path,
            java.time.Instant.now().toString()
        );
    }
}
