package com.eventitta.domain.notification.resolver;

import com.eventitta.domain.notification.constants.AlertConstants;
import com.eventitta.domain.common.exception.CustomException;
import com.eventitta.domain.common.exception.ErrorStatus;
import com.eventitta.domain.notification.domain.AlertLevel;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.net.ConnectException;

@Component
public class AlertLevelResolver {

    public AlertLevel resolveLevel(Exception exception) {
        if (isCriticalException(exception)) {
            return AlertLevel.CRITICAL;
        }
        if (isHighLevelException(exception)) {
            return AlertLevel.HIGH;
        }
        if (isMediumLevelException(exception)) {
            return AlertLevel.MEDIUM;
        }
        return AlertLevel.INFO;
    }

    private boolean isCriticalException(Exception exception) {
        return exception instanceof DataAccessException ||
            exception instanceof ConnectException ||
            hasConnectionRefusedMessage(exception);
    }

    private boolean hasConnectionRefusedMessage(Exception exception) {
        return exception.getMessage() != null &&
            exception.getMessage().contains(AlertConstants.CONNECTION_REFUSED_MESSAGE);
    }

    private boolean isHighLevelException(Exception exception) {
        return isServerErrorException(exception);
    }

    private boolean isServerErrorException(Exception exception) {
        if (!(exception instanceof CustomException customEx)) {
            return false;
        }
        ErrorStatus status = customEx.getErrorCode().defaultHttpStatus();
        return status.is5xxServerError();
    }

    private boolean isMediumLevelException(Exception exception) {
        if (exception instanceof AuthenticationException || exception instanceof AccessDeniedException) {
            return true;
        }
        if (!(exception instanceof CustomException customEx)) {
            return false;
        }
        ErrorStatus status = customEx.getErrorCode().defaultHttpStatus();
        return status.is4xxClientError();
    }
}
