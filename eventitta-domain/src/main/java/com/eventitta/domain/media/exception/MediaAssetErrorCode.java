package com.eventitta.domain.media.exception;

import com.eventitta.domain.common.exception.ErrorCode;
import com.eventitta.domain.common.exception.ErrorStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MediaAssetErrorCode implements ErrorCode {
    NOT_FOUND_MEDIA_ASSET("요청한 미디어 자산을 찾을 수 없습니다.", ErrorStatus.NOT_FOUND),
    MEDIA_ASSET_ACCESS_DENIED("해당 미디어 자산을 사용할 권한이 없습니다.", ErrorStatus.FORBIDDEN),
    MEDIA_ASSET_CATEGORY_MISMATCH("요청한 미디어 자산의 카테고리가 올바르지 않습니다.", ErrorStatus.BAD_REQUEST),
    MEDIA_ASSET_NOT_ATTACHABLE("현재 상태에서는 해당 미디어 자산을 연결할 수 없습니다.", ErrorStatus.BAD_REQUEST),
    DUPLICATE_MEDIA_ASSET("중복된 미디어 자산은 요청할 수 없습니다.", ErrorStatus.BAD_REQUEST),
    TOO_MANY_MEDIA_ASSETS("허용된 미디어 자산 개수를 초과했습니다.", ErrorStatus.BAD_REQUEST);

    private final String message;
    private final ErrorStatus status;

    @Override
    public String defaultMessage() {
        return message;
    }

    @Override
    public ErrorStatus defaultHttpStatus() {
        return status;
    }

    @Override
    public MediaAssetException defaultException() {
        return new MediaAssetException(this);
    }

    @Override
    public MediaAssetException defaultException(Throwable cause) {
        return new MediaAssetException(this, cause);
    }
}
