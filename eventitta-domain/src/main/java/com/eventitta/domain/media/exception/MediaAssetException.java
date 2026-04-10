package com.eventitta.domain.media.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;

public class MediaAssetException extends CustomException {

    public MediaAssetException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MediaAssetException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
