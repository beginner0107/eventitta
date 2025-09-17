package com.eventitta.region.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;

public class RegionException extends CustomException {

    public RegionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RegionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
