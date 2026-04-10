package com.eventitta.domain.file.exception;

import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class FileStorageException extends CustomException {

    public FileStorageException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FileStorageException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
