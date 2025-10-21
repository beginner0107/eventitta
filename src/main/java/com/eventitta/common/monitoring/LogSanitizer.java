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

    // 이메일 패턴: user@example.com
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    // 전화번호 패턴: 010-1234-5678, 01012345678
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("(010|011|016|017|018|019)[-\\s]?\\d{3,4}[-\\s]?\\d{4}");

    // JWT 토큰 패턴: Bearer eyJ... 또는 단독 eyJ...
    private static final Pattern JWT_PATTERN =
        Pattern.compile("(Bearer\\s+)?eyJ[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*");

    // API 키 패턴: 일반적인 API 키 형식 (32-40자 영숫자)
    private static final Pattern API_KEY_PATTERN =
        Pattern.compile("(api[_-]?key|apikey|api_secret|secret[_-]?key)[\"']?\\s*[:=]\\s*[\"']?([A-Za-z0-9]{32,64})");

    // 비밀번호 패턴: password=xxx, pwd=xxx
    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("(password|pwd|passwd)[\"']?\\s*[:=]\\s*[\"']?([^\\s&\"']+)");

    // 신용카드 번호 패턴: 1234-5678-9012-3456
    private static final Pattern CREDIT_CARD_PATTERN =
        Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b");

    private LogSanitizer() {
        // Utility class - 인스턴스화 방지
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

        // 이메일 마스킹: user@example.com → u***@example.com
        sanitized = maskEmail(sanitized);

        // 전화번호 마스킹: 010-1234-5678 → 010-****-5678
        sanitized = maskPhone(sanitized);

        // JWT 토큰 제거: Bearer eyJ... → [JWT_TOKEN_REDACTED]
        sanitized = maskJwt(sanitized);

        // API 키 마스킹: apikey=abc123... → apikey=[API_KEY_REDACTED]
        sanitized = maskApiKey(sanitized);

        // 비밀번호 마스킹: password=secret → password=[REDACTED]
        sanitized = maskPassword(sanitized);

        // 신용카드 번호 마스킹: 1234-5678-9012-3456 → ****-****-****-3456
        sanitized = maskCreditCard(sanitized);

        return sanitized;
    }

    /**
     * 객체를 문자열로 변환 후 마스킹
     */
    public static String sanitize(Object obj) {
        if (obj == null) {
            return "null";
        }
        return sanitize(obj.toString());
    }

    /**
     * 이메일 마스킹: user@example.com → u***@example.com
     */
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

    /**
     * 전화번호 마스킹: 010-1234-5678 → 010-****-5678
     */
    private static String maskPhone(String input) {
        return PHONE_PATTERN.matcher(input).replaceAll(matchResult -> {
            String phone = matchResult.group();
            String digits = phone.replaceAll("[^0-9]", "");

            if (digits.length() == 10) {
                // 01012345678 → 010****5678
                return digits.substring(0, 3) + "****" + digits.substring(7);
            } else if (digits.length() == 11) {
                // 01012345678 → 010****5678
                return digits.substring(0, 3) + "****" + digits.substring(7);
            }

            return "[PHONE_REDACTED]";
        });
    }

    /**
     * JWT 토큰 제거
     */
    private static String maskJwt(String input) {
        return JWT_PATTERN.matcher(input).replaceAll("[JWT_TOKEN_REDACTED]");
    }

    /**
     * API 키 마스킹
     */
    private static String maskApiKey(String input) {
        return API_KEY_PATTERN.matcher(input).replaceAll(matchResult -> {
            String keyName = matchResult.group(1);
            return keyName + "=[API_KEY_REDACTED]";
        });
    }

    /**
     * 비밀번호 마스킹
     */
    private static String maskPassword(String input) {
        return PASSWORD_PATTERN.matcher(input).replaceAll(matchResult -> {
            String fieldName = matchResult.group(1);
            return fieldName + "=[REDACTED]";
        });
    }

    /**
     * 신용카드 번호 마스킹: 1234-5678-9012-3456 → ****-****-****-3456
     */
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

    /**
     * 예외 메시지 마스킹 (스택트레이스는 유지, 메시지만 마스킹)
     */
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
