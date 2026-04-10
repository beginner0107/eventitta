package com.eventitta.domain.region.api.internal.view;

public record RegionReferenceView(
    String code,
    String name,
    String parentCode,
    Integer level
) {
}
