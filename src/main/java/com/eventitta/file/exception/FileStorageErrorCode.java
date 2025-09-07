package com.eventitta.file.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum FileStorageErrorCode implements ErrorCode {
    FILE_SAVE_FAIL("파일 저장 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAIL("파일 삭제 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_LOAD_FAIL("파일 불러오기 실패", HttpStatus.NOT_FOUND),
    FILE_NOT_FOUND("요청한 파일이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    
    INVALID_FILE_REQUEST("잘못된 파일 요청입니다.", HttpStatus.BAD_REQUEST),
    TOO_MANY_FILES("업로드 가능한 파일 개수를 초과했습니다. (최대 5개)", HttpStatus.BAD_REQUEST),
    EMPTY_FILE("빈 파일은 업로드할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_FILENAME("유효하지 않은 파일명입니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE("지원하지 않는 파일 타입입니다.", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("파일 크기가 너무 큽니다. (최대 5MB)", HttpStatus.BAD_REQUEST);

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
