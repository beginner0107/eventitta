package com.eventitta.post.exception;

import com.eventitta.common.exception.CustomException;
import com.eventitta.common.exception.ErrorCode;

public class PostException extends CustomException {
    public PostException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PostException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
