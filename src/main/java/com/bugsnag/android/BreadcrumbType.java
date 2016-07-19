package com.bugsnag.android;

public enum BreadcrumbType {
    ERROR ("error"),
    LOG ("log"),
    MANUAL ("manual"),
    NAVIGATION ("navigation"),
    PROCESS ("process"),
    REQUEST ("request"),
    STATE ("state"),
    USER ("user");

    private final String type;

    BreadcrumbType(String type) {
        this.type = type;
    }

    String serialize() { return type; }
}

