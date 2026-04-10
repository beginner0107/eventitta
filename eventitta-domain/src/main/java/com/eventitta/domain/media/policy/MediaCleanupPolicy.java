package com.eventitta.domain.media.policy;

import java.time.Duration;

public record MediaCleanupPolicy(
    Duration tempRetention,
    Duration releasedRetention
) {
}
