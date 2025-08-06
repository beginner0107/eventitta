package com.eventitta.festivals.domain;

public enum DataSource {
    SEOUL_FESTIVAL("서울시 축제"),
    NATIONAL_FESTIVAL("전국문화축제");

    private final String description;

    DataSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
