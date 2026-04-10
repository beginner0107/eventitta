package com.eventitta.domain.post.exception;

import com.eventitta.domain.common.exception.ErrorCode;
import com.eventitta.domain.common.exception.ErrorStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {
    NOT_FOUND_POST_ID("해당 게시글이 존재하지 않습니다.", ErrorStatus.NOT_FOUND),
    ACCESS_DENIED("해당 게시글을 수정할 권한이 없습니다.", ErrorStatus.FORBIDDEN);
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
    public PostException defaultException() {
        return new PostException(this);
    }

    @Override
    public PostException defaultException(Throwable cause) {
        return new PostException(this, cause);
    }
}
