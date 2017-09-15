package com.bugsnag.android;

import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

final class HandledState {

    @StringDef({ORIGIN_EXCEPTION_HANDLER})
    @Retention(RetentionPolicy.SOURCE)
    @interface SeverityReason {
    }

    static final String ORIGIN_EXCEPTION_HANDLER = "exception_handler";

    @SeverityReason
    private final String severityReasonType;
    private final Severity originalSeverity;
    private final boolean unhandled;

    HandledState(Severity originalSeverity, boolean unhandled) {
        this.originalSeverity = originalSeverity;
        this.severityReasonType = unhandled ? ORIGIN_EXCEPTION_HANDLER : null;
        this.unhandled = unhandled;
    }

    boolean isDefaultSeverity(Severity currentSeverity) {
        return originalSeverity == currentSeverity;
    }

    boolean isUnhandled() {
        return unhandled;
    }

    @Nullable
    String getSeverityReasonType() {
        return severityReasonType;
    }

}
