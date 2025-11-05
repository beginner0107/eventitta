package com.eventitta.notification.resolver;

import com.eventitta.notification.constants.AlertConstants;
import com.eventitta.common.exception.CustomException;
import com.eventitta.notification.domain.AlertLevel;
import org.springframework.dao.DataAccessException;
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
        return customEx.getErrorCode().defaultHttpStatus().is5xxServerError();
    }

    private boolean isMediumLevelException(Exception exception) {
        if (!(exception instanceof CustomException customEx)) {
            return false;
        }
        return customEx.getErrorCode().defaultHttpStatus().is4xxClientError();
    }
}
