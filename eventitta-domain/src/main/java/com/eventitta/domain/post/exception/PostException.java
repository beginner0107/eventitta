package com.eventitta.domain.post.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;

public class PostException extends CustomException {
    public PostException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PostException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
