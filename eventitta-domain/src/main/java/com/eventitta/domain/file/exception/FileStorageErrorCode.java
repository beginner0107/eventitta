package com.eventitta.domain.file.exception;

import com.eventitta.domain.common.exception.ErrorCode;
import com.eventitta.domain.common.exception.ErrorStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FileStorageErrorCode implements ErrorCode {
    FILE_SAVE_FAIL("파일 저장 실패", ErrorStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAIL("파일 삭제 실패", ErrorStatus.INTERNAL_SERVER_ERROR),
    FILE_LOAD_FAIL("파일 불러오기 실패", ErrorStatus.NOT_FOUND),
    FILE_NOT_FOUND("요청한 파일이 존재하지 않습니다.", ErrorStatus.NOT_FOUND),

    INVALID_FILE_REQUEST("잘못된 파일 요청입니다.", ErrorStatus.BAD_REQUEST),
    TOO_MANY_FILES("업로드 가능한 파일 개수를 초과했습니다. (최대 5개)", ErrorStatus.BAD_REQUEST),
    EMPTY_FILE("빈 파일은 업로드할 수 없습니다.", ErrorStatus.BAD_REQUEST),
    INVALID_FILENAME("유효하지 않은 파일명입니다.", ErrorStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE("지원하지 않는 파일 타입입니다.", ErrorStatus.BAD_REQUEST),
    FILE_TOO_LARGE("파일 크기가 너무 큽니다. (최대 5MB)", ErrorStatus.BAD_REQUEST);

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
    public FileStorageException defaultException() {
        return new FileStorageException(this);
    }

    @Override
    public FileStorageException defaultException(Throwable cause) {
        return new FileStorageException(this, cause);
    }
}
