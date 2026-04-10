package com.eventitta.domain.media.domain;

public enum MediaCategory {
    POST_IMAGE("post-image"),
    PROFILE_IMAGE("profile-image");

    private final String storageDirectory;

    MediaCategory(String storageDirectory) {
        this.storageDirectory = storageDirectory;
    }

    public String storageDirectory() {
        return storageDirectory;
    }
}
