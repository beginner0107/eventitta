package com.eventitta.festival.domain;

public enum DataSource {
    SEOUL_CULTURAL_EVENT("서울시 문화행사"),
    NATIONAL_FESTIVAL("전국문화축제");

    private final String description;

    DataSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
