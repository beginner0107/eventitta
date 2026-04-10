package com.eventitta.domain.region.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;

public class RegionException extends CustomException {

    public RegionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RegionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
