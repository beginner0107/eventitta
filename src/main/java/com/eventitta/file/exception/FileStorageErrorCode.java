package com.eventitta.file.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum FileStorageErrorCode implements ErrorCode {
    STORAGE_INIT_FAIL("파일 저장소 초기화 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_SAVE_FAIL("파일 저장 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAIL("파일 삭제 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_LOAD_FAIL("파일 불러오기 실패", HttpStatus.NOT_FOUND),
    FILE_NOT_FOUND("요청한 파일이 존재하지 않습니다.", HttpStatus.NOT_FOUND);

    private final String message;
    private final HttpStatus status;

    @Override
    public String defaultMessage() {
        return message;
    }

    @Override
    public HttpStatus defaultHttpStatus() {
        return status;
    }

    @Override
    public FileStorageException defaultException() {
        return new FileStorageException(this);
    }

    @Override
    public FileStorageException defaultException(Throwable cause) {
        return new FileStorageException(this, cause);
    }
}
