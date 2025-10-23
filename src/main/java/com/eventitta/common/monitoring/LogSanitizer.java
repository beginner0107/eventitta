package com.eventitta.common.monitoring;

import java.util.regex.Pattern;

/**
 * 로그에 민감한 정보가 노출되지 않도록 마스킹하는 유틸리티 클래스
 * <p>
 * 사용 예시:
 * log.info("User info: {}", LogSanitizer.sanitize(userInfo));
 * log.error("Request failed: {}", LogSanitizer.sanitize(errorMessage));
 */
public class LogSanitizer {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("(010|011|016|017|018|019)[-\\s]?\\d{3,4}[-\\s]?\\d{4}");

    private static final Pattern JWT_PATTERN =
        Pattern.compile("(Bearer\\s+)?eyJ[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*");

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(?i)" +
            "(\\b(?:api[_-]?key|apikey|api[_-]?secret|secret[_-]?key)\\b)" +
            "([\"']?)" +
            "(\\s*[:=]\\s*)" +
            "([\"']?)" +
            "([A-Za-z0-9._\\-\\/=]{16,})"
    );

    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("(password|pwd|passwd)[\"']?\\s*[:=]\\s*[\"']?([^\\s&\"']+)");

    private static final Pattern CREDIT_CARD_PATTERN =
        Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b");

    private LogSanitizer() {
    }

    /**
     * 문자열에서 민감한 정보를 마스킹
     *
     * @param input 원본 문자열
     * @return 마스킹된 문자열
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input;
        sanitized = maskEmail(sanitized);
        sanitized = maskPhone(sanitized);
        sanitized = maskJwt(sanitized);
        sanitized = maskApiKey(sanitized);
        sanitized = maskPassword(sanitized);
        sanitized = maskCreditCard(sanitized);
        return sanitized;
    }

    public static String sanitize(Object obj) {
        if (obj == null) {
            return "null";
        }
        return sanitize(obj.toString());
    }

    private static String maskEmail(String input) {
        return EMAIL_PATTERN.matcher(input).replaceAll(matchResult -> {
            String username = matchResult.group(1);
            String domain = matchResult.group(2);

            if (username.length() <= 1) {
                return username + "@" + domain;
            }

            return username.charAt(0) + "***@" + domain;
        });
    }

    private static String maskPhone(String input) {
        return PHONE_PATTERN.matcher(input).replaceAll(matchResult -> {
            String phone = matchResult.group();
            String digits = phone.replaceAll("[^0-9]", "");

            if (digits.length() == 10) {
                return digits.substring(0, 3) + "****" + digits.substring(6);
            } else if (digits.length() == 11) {
                return digits.substring(0, 3) + "****" + digits.substring(7);
            }

            return "[PHONE_REDACTED]";
        });
    }

    private static String maskJwt(String input) {
        return JWT_PATTERN.matcher(input).replaceAll("[JWT_TOKEN_REDACTED]");
    }

    private static String maskApiKey(String input) {
        return API_KEY_PATTERN.matcher(input).replaceAll(m ->
            m.group(1) + m.group(2) + m.group(3) + m.group(4) + "[API_KEY_REDACTED]"
        );
    }

    private static String maskPassword(String input) {
        return PASSWORD_PATTERN.matcher(input).replaceAll(matchResult -> {
            String fieldName = matchResult.group(1);
            return fieldName + "=[REDACTED]";
        });
    }

    private static String maskCreditCard(String input) {
        return CREDIT_CARD_PATTERN.matcher(input).replaceAll(matchResult -> {
            String card = matchResult.group();
            String digits = card.replaceAll("[^0-9]", "");

            if (digits.length() == 16) {
                return "****-****-****-" + digits.substring(12);
            }

            return "[CARD_REDACTED]";
        });
    }

    public static String sanitizeException(Throwable throwable) {
        if (throwable == null) {
            return "null";
        }

        String message = throwable.getMessage();
        if (message == null) {
            return throwable.getClass().getName();
        }

        return throwable.getClass().getName() + ": " + sanitize(message);
    }
}
