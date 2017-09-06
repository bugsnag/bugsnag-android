package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

final class EventHandledState {

    @StringDef({ORIGIN_EXCEPTION_HANDLER})
    @Retention(RetentionPolicy.SOURCE)
    @interface SeverityReason {}

    static final String ORIGIN_EXCEPTION_HANDLER = "exception_handler";

    private final boolean unhandled;
    private final Severity originalSeverity;
    private final String severityReasonType;

    EventHandledState(@SeverityReason @Nullable String severityReasonType,
                      @NonNull Severity originalSeverity) {
        this.severityReasonType = severityReasonType;
        this.unhandled = severityReasonType != null;
        this.originalSeverity = originalSeverity;
    }

    boolean isDefaultSeverity(Severity currentSeverity) {
        return originalSeverity == currentSeverity;
    }

    boolean isUnhandled() {
        return unhandled;
    }

    @Nullable String getSeverityReasonType() {
        return severityReasonType;
    }

}
