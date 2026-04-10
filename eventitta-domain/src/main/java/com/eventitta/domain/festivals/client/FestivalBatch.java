package com.eventitta.domain.festivals.client;

import java.util.List;

public record FestivalBatch<T>(
    List<T> items,
    boolean hasMore
) {
}
