package com.bugsnag.android;

/**
 * Used to inform the NDK library which type of data needs to be updated
 */
public enum NotifyType {
    ALL(1),
    USER(2),
    APP(3),
    DEVICE(4),
    CONTEXT(5),
    RELEASE_STAGES(6),
    FILTERS(7),
    BREADCRUMB(8),
    META(9);

    private Integer intValue;

    NotifyType(Integer intValue) {
        this.intValue = intValue;
    }

    public Integer getValue() {
        return intValue;
    }

    public static NotifyType fromInt(Integer intValue) {
        if (intValue != null) {
            for (NotifyType type : NotifyType.values()) {
                if (intValue.equals(type.getValue())) {
                    return type;
                }
            }
        }
        return null;
    }
}
