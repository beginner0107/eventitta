package com.eventitta.infra.file.service;

import java.net.URI;
import org.springframework.util.StringUtils;

final class StorageKeyUtils {

    private StorageKeyUtils() {
    }

    static String normalizeKey(String rawKeyOrUrl) {
        if (!StringUtils.hasText(rawKeyOrUrl)) {
            return "";
        }

        String candidate = rawKeyOrUrl.trim();
        try {
            if (candidate.contains("://")) {
                candidate = URI.create(candidate).getPath();
            }
        } catch (IllegalArgumentException ignored) {
        }

        candidate = candidate.replace("\\", "/").trim();
        if (candidate.startsWith("/api/v1/uploads/")) {
            candidate = candidate.substring("/api/v1/uploads/".length());
        } else if (candidate.startsWith("/uploads/")) {
            candidate = candidate.substring("/uploads/".length());
        }

        while (candidate.startsWith("/")) {
            candidate = candidate.substring(1);
        }
        return candidate;
    }
}
