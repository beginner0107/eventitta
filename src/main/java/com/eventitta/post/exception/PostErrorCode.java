package com.eventitta.post.exception;

import com.eventitta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {
    NOT_FOUND_POST_ID("해당 게시글이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    ACCESS_DENIED("해당 게시글을 수정할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    ;
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
    public PostException defaultException() {
        return new PostException(this);
    }

    @Override
    public PostException defaultException(Throwable cause) {
        return new PostException(this, cause);
    }
}
