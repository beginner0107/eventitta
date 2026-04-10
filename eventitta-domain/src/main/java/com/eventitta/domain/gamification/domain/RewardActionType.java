package com.eventitta.domain.gamification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RewardActionType {
    CREATE_POST(ActivityType.CREATE_POST),
    CREATE_COMMENT(ActivityType.CREATE_COMMENT),
    JOIN_MEETING(ActivityType.JOIN_MEETING);

    private final ActivityType legacyType;

    public String getDisplayName() {
        return legacyType.getDisplayName();
    }

    public int getDefaultPoint() {
        return legacyType.getDefaultPoint();
    }

    public ResourceType getResourceType() {
        return legacyType.getResourceType();
    }

    public static RewardActionType fromLegacy(ActivityType activityType) {
        return switch (activityType) {
            case CREATE_POST -> CREATE_POST;
            case CREATE_COMMENT -> CREATE_COMMENT;
            case JOIN_MEETING -> JOIN_MEETING;
            default -> throw new IllegalArgumentException("Unsupported reward activity type: " + activityType);
        };
    }
}
