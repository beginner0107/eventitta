package com.eventitta.domain.common.exception;

public enum ErrorStatus {
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409),
    PAYLOAD_TOO_LARGE(413),
    TOO_MANY_REQUESTS(429),
    INTERNAL_SERVER_ERROR(500),
    SERVICE_UNAVAILABLE(503);

    private final int value;

    ErrorStatus(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean is4xxClientError() {
        return value >= 400 && value < 500;
    }

    public boolean is5xxServerError() {
        return value >= 500 && value < 600;
    }
}
