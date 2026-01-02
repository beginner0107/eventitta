package com.eventitta.auth.jwt.service;

import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.jwt.util.JwtTokenUtil;
import com.eventitta.common.util.SecurityUtil;
import com.eventitta.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.eventitta.auth.constants.AuthConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInfoService {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final HttpServletRequest request;

    public String getCurrentUserInfo() {
        String userInfo = SecurityUtil.getCurrentUserInfo();
        if (ANONYMOUS.equals(userInfo)) {
            return getClientIp();
        }
        return userInfo;
    }

    public String extractUserInfoFromRequest(HttpServletRequest request) {
        try {
            return tryExtractUserInfo(request);
        } catch (Exception e) {
            return getClientIp(request);  // ANONYMOUS → IP
        }
    }

    private String tryExtractUserInfo(HttpServletRequest request) {
        String contextUser = getCurrentUserInfo();
        if (!ANONYMOUS.equals(contextUser)) {
            return contextUser;
        }
        return tryTokenExtraction(request);
    }

    private String tryTokenExtraction(HttpServletRequest request) {
        String token = JwtTokenUtil.extractTokenFromRequest(request);
        if (token == null) {
            return getClientIp(request);
        }
        return extractUserFromToken(token, request);
    }

    private String tryStandardTokenParsing(String token) {
        try {
            Long userId = jwtTokenProvider.getUserIdFromExpiredToken(token);
            return getUserInfoIfExists(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private String getUserInfoIfExists(Long userId) {
        if (userId == null) {
            return null;
        }
        return getUserInfoFromDatabase(userId);
    }

    private Long extractUserIdFromTamperedToken(String token) {
        try {
            String[] parts = validateAndSplitToken(token);
            if (parts == null) {
                return null;
            }
            return parseUserIdFromPayload(parts[1]);
        } catch (Exception e) {
            return null;
        }
    }

    private String[] validateAndSplitToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != JWT_PARTS_COUNT) {
            return null;
        }
        return parts;
    }

    private Long parseUserIdFromPayload(String payload) throws Exception {
        String paddedPayload = addBase64Padding(payload);
        String payloadJson = decodeBase64Payload(paddedPayload);
        return extractUserIdFromJson(payloadJson);
    }

    private String addBase64Padding(String payload) {
        int padLength = BASE64_PADDING_BLOCK_SIZE - (payload.length() % BASE64_PADDING_BLOCK_SIZE);
        if (padLength < BASE64_PADDING_BLOCK_SIZE) {
            payload += BASE64_PADDING_CHAR.repeat(padLength);
        }
        return payload;
    }

    private String decodeBase64Payload(String payload) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private Long extractUserIdFromJson(String payloadJson) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(payloadJson);
        JsonNode subNode = jsonNode.get(JWT_SUBJECT_CLAIM);
        if (subNode == null || !subNode.isTextual()) {
            return null;
        }
        return Long.parseLong(subNode.asText());
    }

    private String getUserInfoFromDatabase(Long userId) {
        return userRepository.findById(userId)
            .map(user -> formatUserInfo(user.getEmail(), user.getId()))
            .orElse(formatUnknownUser(userId));
    }


    private String extractUserFromToken(String token, HttpServletRequest request) {
        String userInfo = tryStandardTokenParsing(token);
        if (userInfo != null) {
            return userInfo;
        }

        Long userId = extractUserIdFromTamperedToken(token);
        userInfo = getUserInfoIfExists(userId);
        if (userInfo != null) {
            return userInfo;
        }
        return getClientIp(request);
    }

    private String formatUserInfo(String email, Long id) {
        return String.format(USER_INFO_FORMAT, email, id);
    }

    private String formatUnknownUser(Long userId) {
        return String.format(UNKNOWN_USER_FORMAT, userId);
    }

    private String getClientIp() {
        return getClientIp(this.request);
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
