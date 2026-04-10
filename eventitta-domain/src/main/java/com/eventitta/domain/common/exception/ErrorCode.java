package com.eventitta.domain.common.exception;

public interface ErrorCode {
    String name();

    String defaultMessage();

    ErrorStatus defaultHttpStatus();

    RuntimeException defaultException();

    RuntimeException defaultException(Throwable cause);
}
